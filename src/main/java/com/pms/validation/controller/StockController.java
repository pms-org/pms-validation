package com.pms.validation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.validation.entity.StockEntity;
import com.pms.validation.service.StockService;



@RestController
@RequestMapping("/stocks")
public class StockController {

    @Autowired
    private StockService stockService;
   
    @GetMapping("")
    public List<StockEntity> getAllStocks() {
        return stockService.getAllStockDetails();
    }

    @GetMapping("/{stockId}")
    public StockEntity getStockById(@PathVariable("stockId") UUID stockId) {
        return stockService.getStockById(stockId);
    }

    @GetMapping("/get/sector/{cusipId}")
    public StockEntity getStockByCusipId(@PathVariable("cusipId") String cusipId) {
        return stockService.getStockByCusipId(cusipId);
    }

    @PostMapping("/add/stock")
    public StockEntity addStock(@RequestBody StockEntity stockEntity) {
        return stockService.saveStock(stockEntity);
    }



    
}
