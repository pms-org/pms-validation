package com.pms.validation.repository;

import com.pms.validation.entity.ValidationOutboxEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface ValidationOutboxRepository extends JpaRepository<ValidationOutboxEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ValidationOutboxEntity o WHERE o.status = 'PENDING'")
    List<ValidationOutboxEntity> fetchPending(Sort sort);
}
