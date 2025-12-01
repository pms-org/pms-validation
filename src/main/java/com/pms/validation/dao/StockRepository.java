package com.pms.validation.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.StockEntity;
import com.pms.validation.enums.Sector;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {

    public Optional<StockEntity> findBySymbol(String symbol);

    public List<StockEntity> findBySectorName(Sector sectorName);

}
