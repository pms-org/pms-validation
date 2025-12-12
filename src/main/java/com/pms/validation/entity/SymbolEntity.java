package com.pms.validation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "portfolio_symbol_details")
public class SymbolEntity {
    @Id
    @Column(name = "symbol_id")
    private Long symbolId;

    @Column(name = "symbol")
    private String symbol;
}
