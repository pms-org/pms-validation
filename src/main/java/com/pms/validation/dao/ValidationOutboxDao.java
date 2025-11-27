package com.pms.validation.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pms.validation.entity.ValidationOutbox;

@Repository
public interface ValidationOutboxDao extends JpaRepository<ValidationOutbox, UUID>{
    
}
