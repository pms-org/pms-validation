package com.pms.validation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.SymbolEntity;

@Repository
public interface SymbolRepository extends JpaRepository<SymbolEntity, Long> {

}
