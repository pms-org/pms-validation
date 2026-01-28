package com.pms.validation.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.ValidationOutboxEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

@Repository
public interface ValidationOutboxRepository extends JpaRepository<ValidationOutboxEntity, Long> {

    List<ValidationOutboxEntity> findBySentStatusOrderByValidationOutboxIdAsc(
            String sentStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ValidationOutboxEntity o WHERE o.validationOutboxId = :id")
    ValidationOutboxEntity lockByValidationOutboxId(Long id);

    @Query(value = """
            SELECT *    
            FROM validation_outbox e
            WHERE e.sent_status = 'PENDING'
                AND pg_try_advisory_xact_lock(
                            hashtext('VALIDATION_OUTBOX:' || e.portfolio_id::text)
                        )
            ORDER BY e.created_at
            LIMIT :limit;
                                    """, nativeQuery = true)
    List<ValidationOutboxEntity> findPendingWithPortfolioXactLock(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("update ValidationOutboxEntity e set e.sentStatus = 'SENT' where e.validationOutboxId in :ids")
    void markAsSent(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("update ValidationOutboxEntity e set e.sentStatus = 'FAILED' where e.validationOutboxId = :id")
    void markAsFailed(@Param("id") Long id);
}
