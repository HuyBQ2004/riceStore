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
}
