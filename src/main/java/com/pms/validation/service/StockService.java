package com.pms.validation.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.validation.entity.StockEntity;
import com.pms.validation.enums.Sector;
import com.pms.validation.exception.BadRequestException;
import com.pms.validation.exception.ResourceNotFoundException;
import com.pms.validation.repository.StockRepository;

@Service
public class StockService {
    @Autowired
    private StockRepository stockRepository;

    public List<StockEntity> getAllStockDetails() {
        return stockRepository.findAll();
    }

    public StockEntity getStockById(long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + stockId));
    }

    public StockEntity getStockBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with Symbol: " + symbol));
    }

    public List<StockEntity> getStocksBySector(String sectorName) {
        try {
            Sector sector = Sector.valueOf(sectorName);
            return stockRepository.findBySectorName(sector);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sector: " + sectorName);
        }
    }

    // public StockEntity saveStock(StockEntity stockEntity) {
    // return stockRepository.save(stockEntity);
    // }
}
