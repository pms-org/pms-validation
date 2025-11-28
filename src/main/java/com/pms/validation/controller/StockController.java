package com.pms.validation.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<StockEntity>> getAllStocks() {
        List<StockEntity> list = stockService.getAllStockDetails();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/id/{stockId}")
    public ResponseEntity<StockEntity> getStockById(@PathVariable("stockId") Long stockId) {
        StockEntity stock = stockService.getStockById(stockId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<StockEntity> getStockBySymbol(@PathVariable("symbol") String symbol) {
        StockEntity stock = stockService.getStockBySymbol(symbol);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<StockEntity>> getStockBySector(@PathVariable("sector") String sector) {
        List<StockEntity> stocks = stockService.getStocksBySector(sector);
        return ResponseEntity.ok(stocks);
    }

    // @PostMapping("")
    // public ResponseEntity<StockEntity> addStock(@RequestBody StockEntity
    // stockEntity) {
    // StockEntity created = stockService.saveStock(stockEntity);
    // // return 201 with Location header pointing to new resource id when possible
    // URI location = URI.create(String.format("/stocks/id/%d",
    // created.getStockId()));
    // return ResponseEntity.created(location).body(created);
    // }

}
