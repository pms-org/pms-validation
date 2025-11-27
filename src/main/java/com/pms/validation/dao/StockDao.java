package com.pms.validation.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.StockEntity;

@Repository
public interface StockDao extends JpaRepository<StockEntity, UUID>{

    public Optional<StockEntity> findByCusipId(String cusipId);

}
