package com.pms.validation.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.validation.dao.StockDao;
import com.pms.validation.entity.StockEntity;
import com.pms.validation.enums.Sector;
import com.pms.validation.exception.BadRequestException;
import com.pms.validation.exception.ResourceNotFoundException;

@Service
public class StockService {
    @Autowired
    private StockDao stockDao;

    public List<StockEntity> getAllStockDetails() {
        return stockDao.findAll();
    }

    public StockEntity getStockById(long stockId) {
        return stockDao.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + stockId));
    }

    public StockEntity getStockBySymbol(String symbol) {
        return stockDao.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with Symbol: " + symbol));
    }

    public List<StockEntity> getStocksBySector(String sectorName) {
        try {
            Sector sector = Sector.valueOf(sectorName);
            return stockDao.findBySectorName(sector);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sector: " + sectorName);
        }
    }

    // public StockEntity saveStock(StockEntity stockEntity) {
    // return stockDao.save(stockEntity);
    // }
}
