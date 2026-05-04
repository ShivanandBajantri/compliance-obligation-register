package com.internship.tool.repository;

import com.internship.tool.dto.ComplianceObligationDTO;
import com.internship.tool.dto.ComplianceStatsDTO;
import com.internship.tool.entity.ComplianceObligation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplianceObligationRepository
        extends JpaRepository<ComplianceObligation, Long> {

    // ── Simple filter queries ─────────────────────────────────────────────────

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findByStatus(String status);

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    // ── Paginated full-entity query ───────────────────────────────────────────

    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.readOnly",  value = "true")
    })
    Page<ComplianceObligation> findAll(Pageable pageable);

    // ── Scheduler queries ─────────────────────────────────────────────────────

    @Query("SELECT o FROM ComplianceObligation o " +
           "WHERE o.status != 'COMPLETED' AND o.alertSent = false AND o.dueDate < :today")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findOverdueObligations(@Param("today") LocalDate today);

    @Query("SELECT o FROM ComplianceObligation o " +
           "WHERE o.status != 'COMPLETED' AND o.alertSent = false AND o.dueDate = :dueDate")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findDueSoonObligations(@Param("dueDate") LocalDate dueDate);

    // ── Bulk UPDATE ───────────────────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ComplianceObligation o SET o.alertSent = true WHERE o.id IN :ids")
    void markAlertsSent(@Param("ids") List<Long> ids);

    // ── Count queries ─────────────────────────────────────────────────────────

    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM ComplianceObligation o " +
           "WHERE o.dueDate < :today AND o.status != 'COMPLETED'")
    long countOverdueObligations(@Param("today") LocalDate today);

    @Query("SELECT COUNT(o) FROM ComplianceObligation o " +
           "WHERE o.dueDate BETWEEN :today AND :futureDate AND o.status != 'COMPLETED'")
    long countDueSoonObligations(@Param("today") LocalDate today,
                                 @Param("futureDate") LocalDate futureDate);

    // ── DTO projection queries ────────────────────────────────────────────────

    @Query("SELECT new com.internship.tool.dto.ComplianceObligationDTO(" +
           "o.id, o.title, o.description, o.category, o.status, o.dueDate, " +
           "o.assignedEmail, o.createdAt, o.updatedAt) " +
           "FROM ComplianceObligation o")
    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.readOnly",  value = "true")
    })
    Page<ComplianceObligationDTO> findAllAsDTO(Pageable pageable);

    @Query("SELECT new com.internship.tool.dto.ComplianceObligationDTO(" +
           "o.id, o.title, o.description, o.category, o.status, o.dueDate, " +
           "o.assignedEmail, o.createdAt, o.updatedAt) " +
           "FROM ComplianceObligation o WHERE o.status = :status")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligationDTO> findByStatusAsDTO(@Param("status") String status);

    // ── Keyword search ────────────────────────────────────────────────────────

    @Query("SELECT new com.internship.tool.dto.ComplianceObligationDTO(" +
           "o.id, o.title, o.description, o.category, o.status, o.dueDate, " +
           "o.assignedEmail, o.createdAt, o.updatedAt) " +
           "FROM ComplianceObligation o WHERE " +
           "LOWER(o.title)       LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.category)    LIKE LOWER(CONCAT('%', :keyword, '%'))")
    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.readOnly",  value = "true")
    })
    Page<ComplianceObligationDTO> searchByKeyword(@Param("keyword") String keyword,
                                                   Pageable pageable);

    // ── Dashboard stats — single query ────────────────────────────────────────
    //
    // Bug fix: SUM(CASE WHEN ... THEN 1 ELSE 0 END) returns BigDecimal in H2
    // but Long in PostgreSQL, causing a ClassCastException in the JPQL
    // constructor expression on H2 (constructor takes long primitives).
    // COUNT(CASE WHEN ... THEN 1 END) always returns Long on both databases.

    @Query("SELECT new com.internship.tool.dto.ComplianceStatsDTO(" +
           "COUNT(o), " +
           "COUNT(CASE WHEN o.status = 'PENDING'   THEN 1 END), " +
           "COUNT(CASE WHEN o.status = 'COMPLETED' THEN 1 END), " +
           "COUNT(CASE WHEN o.dueDate < :today     AND o.status != 'COMPLETED' THEN 1 END), " +
           "COUNT(CASE WHEN o.dueDate BETWEEN :today AND :futureDate " +
           "           AND o.status != 'COMPLETED' THEN 1 END)" +
           ") FROM ComplianceObligation o")
    ComplianceStatsDTO getComplianceStats(@Param("today") LocalDate today,
                                          @Param("futureDate") LocalDate futureDate);
}
