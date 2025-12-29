package com.pms.validation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.StockEntity;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {

}
