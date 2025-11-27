package com.pms.validation.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.validation.dao.StockDao;
import com.pms.validation.entity.StockEntity;

@Service
public class StockService {
    @Autowired
    private StockDao stockDao;

    public List<StockEntity> getAllStockDetails() {
      return stockDao.findAll();
    }

    public StockEntity getStockById(UUID stockId) {
        return stockDao.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found with id: " + stockId));
    }

    public StockEntity getStockByCusipId(String cusipId) {
        return stockDao.findByCusipId(cusipId)
                .orElseThrow(() -> new RuntimeException("Stock not found with CUSIP ID: " + cusipId));
    }

    public StockEntity saveStock(StockEntity stockEntity) {
        return stockDao.save(stockEntity);
    }
}
