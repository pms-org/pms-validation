package com.pms.validation.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import com.pms.validation.entity.InvalidTradeEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface InvalidTradeRepository extends JpaRepository<InvalidTradeEntity, Long> {

    List<InvalidTradeEntity> findBySentStatusOrderByInvalidTradeOutboxIdAsc(String sentStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM InvalidTradeEntity o WHERE o.invalidTradeOutboxId = :id")
    InvalidTradeEntity lockByInvalidTradeOutboxId(Long id);

    @Query(value = """
            SELECT *
            FROM validation_invalid_trades e
            WHERE e.sent_status = 'PENDING'
                AND pg_try_advisory_xact_lock(
                            hashtext('INVALID_TRADE_OUTBOX:' || e.portfolio_id::text)
                        )
            ORDER BY e.created_at
            LIMIT :limit;
                                    """, nativeQuery = true)
    List<InvalidTradeEntity> findPendingWithPortfolioXactLock(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("update InvalidTradeEntity e set e.sentStatus = 'SENT' where e.invalidTradeOutboxId in :ids")
    void markAsSent(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("update InvalidTradeEntity e set e.sentStatus = 'FAILED' where e.invalidTradeOutboxId = :id")
    void markAsFailed(@Param("id") Long id);
}
