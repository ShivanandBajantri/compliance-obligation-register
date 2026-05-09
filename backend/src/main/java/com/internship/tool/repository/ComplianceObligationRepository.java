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

/**
 * Repository for ComplianceObligation.
 *
 * Performance notes:
 * - @EntityGraph is intentionally NOT used here because ComplianceObligation has no
 *   lazy-loaded associations. @EntityGraph only helps with association fetching (e.g.
 *   @OneToMany, @ManyToMany). Applying it to scalar columns has no effect and adds noise.
 * - All list queries that are read-only carry the hibernate.readOnly hint so Hibernate
 *   skips dirty-checking on the returned objects.
 * - Bulk DML (markAlertsSent) uses @Modifying to execute as a single UPDATE statement
 *   instead of loading each entity and saving it individually.
 * - DTO projections (JPQL constructor expressions) avoid loading full entities when only
 *   a subset of columns is needed.
 * - The single-query stats aggregation (getComplianceStats) replaces five separate
 *   count queries that were previously issued by the service layer.
 */
@Repository
public interface ComplianceObligationRepository
        extends JpaRepository<ComplianceObligation, Long> {

    // -------------------------------------------------------------------------
    // Simple filter queries
    // -------------------------------------------------------------------------

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findByStatus(String status);

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    // -------------------------------------------------------------------------
    // Paginated full-entity query (used by /all endpoint)
    // -------------------------------------------------------------------------

    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.readOnly",  value = "true")
    })
    Page<ComplianceObligation> findAll(Pageable pageable);

    // -------------------------------------------------------------------------
    // Scheduler queries — filtered to only unprocessed, non-completed rows
    // so the index on (status, alert_sent) is used.
    // -------------------------------------------------------------------------

    @Query("SELECT o FROM ComplianceObligation o " +
           "WHERE o.status != 'COMPLETED' AND o.alertSent = false AND o.dueDate < :today")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findOverdueObligations(@Param("today") LocalDate today);

    @Query("SELECT o FROM ComplianceObligation o " +
           "WHERE o.status != 'COMPLETED' AND o.alertSent = false AND o.dueDate = :dueDate")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<ComplianceObligation> findDueSoonObligations(@Param("dueDate") LocalDate dueDate);

    // -------------------------------------------------------------------------
    // Bulk UPDATE — @Modifying is required for any DML statement.
    // Without it Spring Data throws an InvalidDataAccessApiUsageException.
    // clearAutomatically = true keeps the first-level cache consistent.
    // -------------------------------------------------------------------------

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ComplianceObligation o SET o.alertSent = true WHERE o.id IN :ids")
    void markAlertsSent(@Param("ids") List<Long> ids);

    // -------------------------------------------------------------------------
    // Count queries for dashboard stats
    // -------------------------------------------------------------------------

    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM ComplianceObligation o " +
           "WHERE o.dueDate < :today AND o.status != 'COMPLETED'")
    long countOverdueObligations(@Param("today") LocalDate today);

    @Query("SELECT COUNT(o) FROM ComplianceObligation o " +
           "WHERE o.dueDate BETWEEN :today AND :futureDate AND o.status != 'COMPLETED'")
    long countDueSoonObligations(@Param("today") LocalDate today,
                                 @Param("futureDate") LocalDate futureDate);

    // -------------------------------------------------------------------------
    // DTO projection queries — constructor expressions select only the columns
    // that are actually needed, reducing data transfer and skipping dirty-check.
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Full-text keyword search with DTO projection and pagination.
    // LOWER + LIKE is not index-friendly for large tables; for production scale
    // consider a full-text search extension (pg_trgm / Elasticsearch).
    // -------------------------------------------------------------------------

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

    // ── Dashboard stats ───────────────────────────────────────────────────────
    // Removed the single-query JPQL aggregation — COUNT(CASE WHEN) / SUM(CASE WHEN)
    // inside a constructor expression is not portable between H2 and PostgreSQL.
    // Stats are now assembled in the service layer from the individual count
    // queries above (countByStatus, countOverdueObligations, countDueSoonObligations).
}
