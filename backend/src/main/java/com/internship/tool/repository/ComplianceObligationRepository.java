package com.internship.tool.repository;

import com.internship.tool.entity.ComplianceObligation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
git 
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplianceObligationRepository 
        extends JpaRepository<ComplianceObligation, Long> {

    List<ComplianceObligation> findByStatus(String status);

    List<ComplianceObligation> findByDueDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );
}

// This repository interface extends JpaRepository, providing CRUD operations for ComplianceObligation entities.
// It also includes custom query methods to find obligations by status and due date range.