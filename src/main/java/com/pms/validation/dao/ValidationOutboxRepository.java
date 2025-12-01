package com.pms.validation.dao;

import com.pms.validation.entity.ValidationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ValidationOutboxRepository extends JpaRepository<ValidationOutboxEntity, Long> {
    List<ValidationOutboxEntity> findByStatusAndUpdatedAtBefore(String status, LocalDateTime before);
}
