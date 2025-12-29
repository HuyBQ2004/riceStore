package swp.se1889.g1.rice_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import swp.se1889.g1.rice_store.entity.Invoices;
import swp.se1889.g1.rice_store.entity.Store;
import swp.se1889.g1.rice_store.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<Invoices, Long>, JpaSpecificationExecutor<Invoices> {
    List<Invoices> findByType(Invoices.InvoiceType type);

    Page<Invoices> findByStoreAndType(Store store, Invoices.InvoiceType type, Pageable pageable);

    List<Invoices> findByStoreId(Long storeID);

    Page<Invoices> findById(Long id, Pageable pageable);

    Page<Invoices> findInvoicesByStore(Store store, Pageable pageable);

    // --- A. Tổng hóa đơn hôm nay ---
    @Query("SELECT COUNT(i) FROM Invoices i WHERE i.store.id = :storeId AND i.type = 'Sale' AND i.isDeleted = false AND i.createdAt BETWEEN :start AND :end")
    long countTodayInvoices(@Param("storeId") Long storeId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    // --- B. Tổng doanh thu hôm nay ---
    @Query("SELECT COALESCE(SUM(i.finalAmount), 0) FROM Invoices i WHERE i.store.id = :storeId AND i.type = 'Sale' AND i.isDeleted = false AND i.createdAt BETWEEN :start AND :end")
    BigDecimal sumTodayRevenue(@Param("storeId") Long storeId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // --- C. Doanh thu theo thứ trong tuần hiện tại ---
    @Query(value = "SELECT DATEPART(WEEKDAY, i.created_at) AS weekday, SUM(i.final_amount) AS revenue FROM invoices i WHERE i.store_id = :storeId AND i.type = 'Sale' AND i.is_deleted = 0 AND i.created_at BETWEEN :start AND :end GROUP BY DATEPART(WEEKDAY, i.created_at) ORDER BY weekday", nativeQuery = true)
    List<Object[]> getRevenueByWeekday(@Param("storeId") Long storeId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // --- D. Doanh thu theo các tháng trong năm hiện tại ---
    @Query(value = "SELECT MONTH(i.created_at) AS month, SUM(i.final_amount) AS revenue FROM invoices i WHERE i.store_id = :storeId AND i.type = 'Sale' AND i.is_deleted = 0 AND YEAR(i.created_at) = :year GROUP BY MONTH(i.created_at) ORDER BY month", nativeQuery = true)
    List<Object[]> getRevenueByMonth(@Param("storeId") Long storeId, @Param("year") int year);
    // =================================================================================
    // SCENARIO 1: SIMPLE READ (Đọc đơn giản 1 bản ghi)
    // Mục tiêu: Đo overhead của Hibernate khi mapping 1 dòng dữ liệu đơn lẻ.
    // =================================================================================

    // 1.1 JPQL Approach
    @Query("SELECT i FROM Invoices i WHERE i.id = :id")
    Optional<Invoices> findByIdJPQL(@Param("id") Long id);

    // 1.2 Native SQL Approach
    @Query(value = "SELECT * FROM invoices WHERE id = :id", nativeQuery = true)
    Optional<Invoices> findByIdNative(@Param("id") Long id);


    // =================================================================================
    // SCENARIO 2: COMPLEX AGGREGATION (Tính toán báo cáo doanh thu theo tháng)
    // Mục tiêu: Chứng minh sức mạnh của Native SQL khi xử lý Group By và Date Function.
    // Logic: Tính tổng doanh thu theo từng tháng trong năm cụ thể của 1 cửa hàng.
    // =================================================================================

    // 2.1 JPQL Approach (Hibernate 6 hỗ trợ hàm MONTH/YEAR trong HQL)
    @Query("SELECT MONTH(i.createdAt) as month, SUM(i.finalAmount) as revenue " +
            "FROM Invoices i " +
            "WHERE i.store.id = :storeId " +
            "AND i.type = 'Sale' " +
            "AND i.isDeleted = false " +
            "AND YEAR(i.createdAt) = :year " +
            "GROUP BY MONTH(i.createdAt) " +
            "ORDER BY month")
    List<Object[]> getRevenueByMonthJPQL(@Param("storeId") Long storeId, @Param("year") int year);

    // 2.2 Native SQL Approach (Tối ưu hóa cho SQL Server)
    @Query(value = "SELECT MONTH(i.created_at) AS month, SUM(i.final_amount) AS revenue " +
            "FROM invoices i " +
            "WHERE i.store_id = :storeId " +
            "AND i.type = 'Sale' " +
            "AND i.is_deleted = 0 " +
            "AND YEAR(i.created_at) = :year " +
            "GROUP BY MONTH(i.created_at) " +
            "ORDER BY month", nativeQuery = true)
    List<Object[]> getRevenueByMonthNative(@Param("storeId") Long storeId, @Param("year") int year);


    // =================================================================================
    // SCENARIO 3: N+1 PROBLEM SIMULATION (Liệt kê danh sách lớn)
    // Mục tiêu: Đo lượng RAM (Memory Footprint) và CPU.
    // Logic: Lấy 5000 hóa đơn mới nhất của cửa hàng để hiển thị lên bảng.
    // =================================================================================

    // 3.1 JPQL Approach (Dễ dính N+1 nếu truy cập vào i.customer hoặc i.store sau đó)
    @Query("SELECT i FROM Invoices i WHERE i.store.id = :storeId ORDER BY i.createdAt DESC ")
    List<Invoices> findTop5000ByStoreJPQL(@Param("storeId") Long storeId, Pageable pageable);

    // 3.2 Native SQL Approach (Chỉ lấy dữ liệu cột cần thiết - Projection, nhẹ hơn nhiều)
    @Query(value = "SELECT top 5000 i.id, i.final_amount, i.created_at, i.payment_status " +
            "FROM invoices i " +
            "WHERE i.store_id = :storeId " +
            "ORDER BY i.created_at DESC", nativeQuery = true)
    List<Object[]> findTop5000ByStoreNative(@Param("storeId") Long storeId);

    // [NEW] 3.3 JPQL Optimized (Dùng JOIN FETCH để khử N+1)
    // Lưu ý: Vì Invoice có quan hệ với Store, Customer, và User (createdBy),
    // ta cần JOIN FETCH hết các quan hệ EAGER này.
    @Query("SELECT i FROM Invoices i " +
            "JOIN FETCH i.store " +
            "JOIN FETCH i.customer " +
            "JOIN FETCH i.createdBy " +
            "WHERE i.store.id = :storeId " +
            "ORDER BY i.createdAt DESC "
    )
    List<Invoices> findTop5000ByStoreJPQLOptimized(@Param("storeId") Long storeId, Pageable pageable);
}
