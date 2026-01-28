package com.pms.validation.repository;

import java.util.UUID;

import com.pms.validation.entity.DlqEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DlqRepository extends JpaRepository<DlqEntry, Long> {

}
