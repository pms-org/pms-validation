package com.pms.validation.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.ValidationOutboxEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface ValidationOutboxRepository extends JpaRepository<ValidationOutboxEntity, Long> {

    List<ValidationOutboxEntity> findBySentStatusOrderByValidationOutboxIdAsc(
        String sentStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ValidationOutboxEntity o WHERE o.validationOutboxId = :id")
    ValidationOutboxEntity lockByValidationOutboxId(Long id);
}
