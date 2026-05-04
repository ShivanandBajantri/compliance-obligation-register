package com.internship.tool.service;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    /**
     * Admin email for weekly summary reports.
     * Configurable via app.admin-email property (defaults to a placeholder).
     */
    @Value("${app.admin-email:admin@company.com}")
    private String adminEmail;

    public AlertScheduler(ComplianceObligationRepository repository, EmailService emailService) {
        this.repository   = repository;
        this.emailService = emailService;
    }

    /**
     * Daily check at 9 AM — sends alerts for overdue and due-soon obligations.
     *
     * Bug fixes applied:
     * - Null check on assignedEmail before sending (prevents NPE)
     * - markAlertsSent() called only after all emails are attempted
     * - @Transactional ensures the bulk UPDATE is committed atomically
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkObligations() {
        logger.info("Starting daily obligation check");

        LocalDate today       = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        List<ComplianceObligation> overdueObligations  = repository.findOverdueObligations(today);
        List<ComplianceObligation> dueSoonObligations  = repository.findDueSoonObligations(dueSoonDate);

        // ── Overdue ──────────────────────────────────────────────────────────
        if (!overdueObligations.isEmpty()) {
            List<Long> overdueIds = overdueObligations.stream()
                    .map(ComplianceObligation::getId)
                    .collect(Collectors.toList());

            overdueObligations.forEach(this::sendOverdueAlert);
            repository.markAlertsSent(overdueIds);
            logger.info("Processed {} overdue obligations", overdueIds.size());
        }

        // ── Due soon ─────────────────────────────────────────────────────────
        if (!dueSoonObligations.isEmpty()) {
            List<Long> dueSoonIds = dueSoonObligations.stream()
                    .map(ComplianceObligation::getId)
                    .collect(Collectors.toList());

            dueSoonObligations.forEach(this::sendDueSoonAlert);
            repository.markAlertsSent(dueSoonIds);
            logger.info("Processed {} due-soon obligations", dueSoonIds.size());
        }

        logger.info("Daily obligation check completed");
    }

    /**
     * Weekly summary every Monday at 9 AM.
     *
     * Bug fixes applied:
     * - Added @Transactional(readOnly = true) — was missing, queries ran outside transaction
     * - Admin email is now configurable via ${app.admin-email} property
     * - String concatenation replaced with StringBuilder to avoid O(n²) allocation
     */
    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklySummary() {
        logger.info("Sending weekly summary to {}", adminEmail);

        long totalPending = repository.countByStatus("PENDING");

        if (totalPending == 0) {
            logger.info("No pending obligations — skipping weekly summary");
            return;
        }

        List<ComplianceObligation> pendingObligations = repository.findByStatus("PENDING");

        StringBuilder body = new StringBuilder();
        body.append(String.format("Total pending obligations: %d%n%n", totalPending));
        for (ComplianceObligation o : pendingObligations) {
            body.append(String.format("- %s (Due: %s)%n", o.getTitle(), o.getDueDate()));
        }

        emailService.sendEmail(adminEmail, "Weekly Compliance Summary", body.toString());
        logger.info("Weekly summary sent with {} pending obligations", totalPending);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendOverdueAlert(ComplianceObligation obligation) {
        // Bug fix: skip if no email address is set
        if (obligation.getAssignedEmail() == null || obligation.getAssignedEmail().isBlank()) {
            logger.warn("Obligation {} has no assigned email — skipping overdue alert",
                    obligation.getId());
            return;
        }

        String message = String.format(
                "The following compliance obligation is overdue:%n%n" +
                "Title:    %s%n" +
                "Due Date: %s%n" +
                "Status:   %s%n%n" +
                "Please take immediate action.",
                obligation.getTitle(), obligation.getDueDate(), obligation.getStatus());

        emailService.sendEmail(obligation.getAssignedEmail(), "Overdue Compliance Alert", message);
        logger.info("Overdue alert sent for obligation {}", obligation.getId());
    }

    private void sendDueSoonAlert(ComplianceObligation obligation) {
        // Bug fix: skip if no email address is set
        if (obligation.getAssignedEmail() == null || obligation.getAssignedEmail().isBlank()) {
            logger.warn("Obligation {} has no assigned email — skipping due-soon alert",
                    obligation.getId());
            return;
        }

        String message = String.format(
                "The following compliance obligation is due in 7 days:%n%n" +
                "Title:    %s%n" +
                "Due Date: %s%n" +
                "Status:   %s%n%n" +
                "Please prepare accordingly.",
                obligation.getTitle(), obligation.getDueDate(), obligation.getStatus());

        emailService.sendEmail(obligation.getAssignedEmail(), "Upcoming Compliance Alert", message);
        logger.info("Due-soon alert sent for obligation {}", obligation.getId());
    }
}
