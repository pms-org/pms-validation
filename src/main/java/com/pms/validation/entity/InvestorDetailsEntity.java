package com.pms.validation.entity;

import java.util.UUID;

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
@Table(name = "portfolio_investor_details")
public class InvestorDetailsEntity {
    @Id
    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "name")
    private String name;

    @Column(name = "number")
    private Long number;
    
    @Column(name = "address")
    private String address;
}
