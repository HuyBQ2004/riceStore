package swp.se1889.g1.rice_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import swp.se1889.g1.rice_store.entity.DebtRecords;
import swp.se1889.g1.rice_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface DebtRecordRepository extends JpaRepository<DebtRecords, Long>, JpaSpecificationExecutor<DebtRecords> {
    //List<DebtRecords> findByUserId(User createdBy);
    Optional<DebtRecords> findById(Long id);

    List<DebtRecords> findByCustomerId(Long customerId);

    Page<DebtRecords> findByCustomerId(Long customerId, Pageable pageable);
}
