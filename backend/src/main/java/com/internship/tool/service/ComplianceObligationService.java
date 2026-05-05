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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ComplianceObligationService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceObligationService.class);

    /**
     * Allowed status values — enforced on create and update to prevent
     * typos like "COMPELTED" silently entering the database.
     */
    private static final Set<String> VALID_STATUSES =
            Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    public ComplianceObligationService(ComplianceObligationRepository repository,
                                       EmailService emailService) {
        this.repository   = repository;
        this.emailService = emailService;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public ComplianceObligation create(ComplianceObligation obligation) {
        obligation.setCreatedAt(LocalDateTime.now());
        obligation.setUpdatedAt(LocalDateTime.now());
        obligation.setStatus("PENDING");   // always start as PENDING
        obligation.setAlertSent(false);

        ComplianceObligation saved = repository.save(obligation);

        // Fire-and-forget — does not block the HTTP response or the transaction.
        // Bug fix: was called synchronously inside @Transactional, causing the
        // SMTP round-trip to delay every create response.
        if (saved.getAssignedEmail() != null && !saved.getAssignedEmail().isBlank()) {
            sendAssignmentEmail(saved);
        }

        return saved;
    }

    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public ComplianceObligation update(Long id, ComplianceObligation updated) {
        ComplianceObligation existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obligation not found: " + id));

        // Validate status value before persisting
        if (updated.getStatus() != null && !VALID_STATUSES.contains(updated.getStatus())) {
            throw new IllegalArgumentException(
                    "Invalid status '" + updated.getStatus() +
                    "'. Allowed values: " + VALID_STATUSES);
        }

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setStatus(updated.getStatus());
        existing.setDueDate(updated.getDueDate());
        existing.setAssignedEmail(updated.getAssignedEmail());
        existing.setAlertSent(updated.isAlertSent());
        existing.setUpdatedAt(LocalDateTime.now());

        return repository.save(existing);
    }

    @Transactional
    @CacheEvict(value = {"obligationCount", "complianceStats"}, allEntries = true)
    public String delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Obligation not found: " + id);
        }
        repository.deleteById(id);
        return "Deleted";
    }

    // ── Read operations ───────────────────────────────────────────────────────

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

    /** Unbounded fetch — used only by CSV export (streams in pages of 500). */
    public List<ComplianceObligation> getAll() {
        return repository.findAll();
    }

    @Cacheable(value = "obligationCount", key = "'total'")
    public long count() {
        return repository.count();
    }

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

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Send assignment notification email.
     *
     * Annotated @Async so it runs in a separate thread and does not block
     * the HTTP response or hold the database transaction open while waiting
     * for the SMTP server.
     *
     * Requires @EnableAsync on the application class (already present via
     * @SpringBootApplication which picks up @EnableScheduling — @EnableAsync
     * is added to ComplianceObligationRegisterApplication).
     */
    @Async
    public void sendAssignmentEmail(ComplianceObligation obligation) {
        try {
            String subject = "New Compliance Obligation Assigned";
            String body = String.format(
                    "A new compliance obligation has been assigned to you:%n%n" +
                    "Title:    %s%n" +
                    "Due Date: %s%n%n" +
                    "Please review and complete it as required.",
                    obligation.getTitle(), obligation.getDueDate());
            emailService.sendEmail(obligation.getAssignedEmail(), subject, body);
        } catch (Exception e) {
            logger.error("Failed to send assignment email for obligation {}: {}",
                    obligation.getId(), e.getMessage());
        }
    }
}
