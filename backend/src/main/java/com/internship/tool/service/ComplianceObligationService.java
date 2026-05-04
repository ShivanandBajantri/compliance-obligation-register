package com.internship.tool.service;

import com.internship.tool.dto.ComplianceObligationDTO;
import com.internship.tool.dto.ComplianceStatsDTO;
import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for ComplianceObligation.
 *
 * Performance decisions:
 * - Class-level @Transactional(readOnly = true) sets the default for all methods.
 *   Read-only transactions skip dirty-checking and allow the JDBC driver / DB to
 *   apply read optimisations (e.g. read replicas, snapshot isolation).
 * - Write methods override with @Transactional (readOnly = false) so they get a
 *   full read-write transaction.  Previously update() and delete() were missing
 *   this override, meaning changes were never committed.
 * - Cache eviction on writes keeps the stats/count caches consistent.
 * - getAll() (no-arg, unbounded) is kept for the CSV export but callers should
 *   prefer the paginated getAllAsDTO() for normal list views.
 */
@Service
@Transactional(readOnly = true)
public class ComplianceObligationService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceObligationService.class);

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    public ComplianceObligationService(ComplianceObligationRepository repository,
                                       EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public ComplianceObligation create(ComplianceObligation obligation) {
        obligation.setCreatedAt(LocalDateTime.now());
        obligation.setUpdatedAt(LocalDateTime.now());
        obligation.setStatus("PENDING");
        obligation.setAlertSent(false);

        ComplianceObligation saved = repository.save(obligation);

        if (obligation.getAssignedEmail() != null && !obligation.getAssignedEmail().isBlank()) {
            sendEmailNotificationAsync(saved);
        }

        return saved;
    }

    /**
     * Update an existing obligation.
     *
     * The method loads the entity inside the same transaction so Hibernate's
     * dirty-checking mechanism writes only the changed columns on commit —
     * no explicit repository.save() call is needed (though it is harmless).
     * The @Transactional annotation was previously missing, which meant the
     * changes were never flushed to the database.
     */
    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public ComplianceObligation update(Long id, ComplianceObligation updated) {
        ComplianceObligation existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obligation not found: " + id));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setStatus(updated.getStatus());
        existing.setDueDate(updated.getDueDate());
        existing.setAssignedEmail(updated.getAssignedEmail());
        existing.setAlertSent(updated.isAlertSent());
        existing.setUpdatedAt(LocalDateTime.now());

        // Dirty-checking will flush the changes; explicit save() is kept for clarity.
        return repository.save(existing);
    }

    /**
     * Delete an obligation by id.
     *
     * Uses deleteById to avoid the extra SELECT that repository.delete(entity)
     * requires when the entity is not already in the first-level cache.
     * The @Transactional annotation was previously missing.
     */
    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public String delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Obligation not found: " + id);
        }
        repository.deleteById(id);
        return "Deleted";
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    public List<ComplianceObligation> getByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<ComplianceObligationDTO> getByStatusAsDTO(String status) {
        return repository.findByStatusAsDTO(status);
    }

    public ComplianceObligation getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obligation not found: " + id));
    }

    public Page<ComplianceObligation> getAll(Pageable pageable) {
        logger.debug("Retrieving obligations page={} size={} sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return repository.findAll(pageable);
    }

    public Page<ComplianceObligationDTO> getAllAsDTO(Pageable pageable) {
        logger.debug("Retrieving obligations DTO page={} size={} sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return repository.findAllAsDTO(pageable);
    }

    /**
     * Unbounded fetch — only used for CSV export.
     * For large datasets consider replacing with a streaming / chunked approach.
     */
    public List<ComplianceObligation> getAll() {
        return repository.findAll();
    }

    @Cacheable(value = "obligationCount", key = "'total'")
    public long count() {
        return repository.count();
    }

    /**
     * Single-query dashboard stats — one DB round-trip instead of five.
     * Result is cached; cache is evicted on any write operation.
     */
    @Cacheable(value = "complianceStats", key = "'stats'")
    public ComplianceStatsDTO getStats() {
        LocalDate today      = LocalDate.now();
        LocalDate futureDate = today.plusDays(7);
        return repository.getComplianceStats(today, futureDate);
    }

    public Page<ComplianceObligationDTO> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAllAsDTO(pageable);
        }
        return repository.searchByKeyword(keyword.trim(), pageable);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void sendEmailNotificationAsync(ComplianceObligation obligation) {
        try {
            String subject = "New Obligation Assigned";
            String message = String.format(
                    "A new compliance obligation has been assigned to you:%n%n" +
                    "Title: %s%nDue Date: %s%n%n" +
                    "Please review and complete as required.",
                    obligation.getTitle(), obligation.getDueDate());

            emailService.sendEmail(obligation.getAssignedEmail(), subject, message);
        } catch (Exception e) {
            logger.error("Failed to send email notification for obligation {}: {}",
                    obligation.getId(), e.getMessage());
        }
    }
}
