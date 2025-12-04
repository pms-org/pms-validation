package com.pms.validation.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.InvalidTradeEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface InvalidTradeRepository extends JpaRepository<InvalidTradeEntity, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM InvalidTradeEntity o WHERE o.sentStatus = 'PENDING'")
    List<InvalidTradeEntity> fetchPending(Sort sort);
}
