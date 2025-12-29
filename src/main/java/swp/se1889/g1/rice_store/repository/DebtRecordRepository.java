package swp.se1889.g1.rice_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp.se1889.g1.rice_store.entity.DebtRecords;
import swp.se1889.g1.rice_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface DebtRecordRepository extends JpaRepository<DebtRecords, Long>, JpaSpecificationExecutor<DebtRecords> {
    //List<DebtRecords> findByUserId(User createdBy);


    List<DebtRecords> findByCustomerId(Long customerId);

    Page<DebtRecords> findByCustomerId(Long customerId, Pageable pageable);

    // =================================================================================
    // SCENARIO 1: SIMPLE READ (Đọc 1 dòng theo ID)
    // Mục tiêu: Đo overhead khởi tạo của Hibernate với 1 Entity đơn lẻ.
    // =================================================================================

    // 1.1 JPQL/JPA Standard (Có sẵn findById trong JpaRepository)
    // Hibernate sẽ dùng Cache L1 nếu có.
    Optional<DebtRecords> findById(Long id);

    // 1.2 Native SQL Approach
    // Lưu ý: Dù Native nhưng trả về Entity class, Hibernate vẫn phải map kết quả vào Object.
    @Query(value = "SELECT * FROM debt_records WHERE id = :id", nativeQuery = true)
    Optional<DebtRecords> findByIdNative(@Param("id") Long id);
// =================================================================================
    // SCENARIO 2: COMPLEX AGGREGATION (Báo cáo công nợ theo thời gian)
    // Mục tiêu: So sánh hiệu năng Group By và Hàm thời gian (YEAR/MONTH)
    // Logic: Thống kê tổng nợ của khách hàng theo từng tháng.
    // =================================================================================

    // 1. Native SQL Approach (Đã tối ưu)
    // Ưu điểm: SQL Server thực hiện gom nhóm và trả về kết quả thô cực nhanh.
    @Query(value = """
            SELECT 
                YEAR(d.create_on) AS year,
                MONTH(d.create_on) AS month,
                SUM(d.amount) AS total_debt
            FROM debt_records d
            WHERE d.customer_id = :customerId
              AND d.is_deleted = 0
            GROUP BY YEAR(d.create_on), MONTH(d.create_on)
            ORDER BY year, month
            """, nativeQuery = true)
    List<Object[]> getMonthlyDebtNative(@Param("customerId") Long customerId);

    // 2. JPQL Approach (Đối thủ so sánh)
    // Nhược điểm: Hibernate phải parse hàm YEAR/MONTH của HQL sang SQL dialect tương ứng.
    @Query("""
            SELECT 
                YEAR(d.createOn) as year, 
                MONTH(d.createOn) as month, 
                SUM(d.amount) as totalDebt
            FROM DebtRecords d
            WHERE d.customerId = :customerId
              AND d.isDelete = false
            GROUP BY YEAR(d.createOn), MONTH(d.createOn)
            ORDER BY year, month
            """)
    List<Object[]> getMonthlyDebtJPQL(@Param("customerId") Long customerId);


    // =================================================================================
    // SCENARIO 3: LARGE DATA FETCH (Lấy lịch sử nợ dài)
    // Mục tiêu: Đo Memory footprint (RAM) khi load danh sách lớn.
    // Logic: Lấy 5000 giao dịch nợ gần nhất của 1 khách hàng VIP.
    // =================================================================================

    // 3. Native SQL (Projection - Chỉ lấy field cần thiết)
    @Query(value = """
            SELECT TOP 5000 id, amount, note, create_on 
            FROM debt_records 
            WHERE customer_id = :customerId 
            ORDER BY create_on DESC
            """, nativeQuery = true)
    List<Object[]> getHistoryNative(@Param("customerId") Long customerId);

    // 4. JPQL (Entity Loading - Load nguyên object)
    // Rủi ro: Tốn RAM để quản lý State của 5000 object DebtRecords.
    @Query("""
            SELECT d 
            FROM DebtRecords d 
            WHERE d.customerId = :customerId 
            ORDER BY d.createOn DESC 
            
            """)
    List<DebtRecords> getHistoryJPQL(@Param("customerId") Long customerId, Pageable pageable);
}
