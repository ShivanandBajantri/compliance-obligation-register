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
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    @Value("${app.admin-email:admin@company.com}")
    private String adminEmail;

    public AlertScheduler(ComplianceObligationRepository repository, EmailService emailService) {
        this.repository   = repository;
        this.emailService = emailService;
    }

    // ── Scheduled daily trigger ───────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkObligations() {
        logger.info("Scheduled daily obligation check started");
        Map<String, Integer> result = sendAlerts();
        logger.info("Scheduled daily check completed — overdue={} dueSoon={}",
                result.get("overdue"), result.get("dueSoon"));
    }

    // ── Manual trigger (called from controller) ───────────────────────────────

    /**
     * Send alert emails to all overdue and due-soon obligations that have
     * an assigned email and have not yet been alerted.
     *
     * Returns a summary map: { overdue: N, dueSoon: N, skipped: N }
     * — skipped = obligations with no assignedEmail.
     *
     * This method is @Transactional so the bulk markAlertsSent() UPDATE
     * is committed atomically with the email dispatch loop.
     */
    @Transactional
    public Map<String, Integer> sendAlerts() {
        LocalDate today       = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        List<ComplianceObligation> overdue = repository.findOverdueObligations(today);
        List<ComplianceObligation> dueSoon = repository.findDueSoonObligations(today, dueSoonDate);

        int sentOverdue  = 0;
        int sentDueSoon  = 0;
        int skipped      = 0;

        // ── Overdue alerts ────────────────────────────────────────────────────
        if (!overdue.isEmpty()) {
            List<Long> ids = overdue.stream().map(ComplianceObligation::getId).collect(Collectors.toList());
            for (ComplianceObligation o : overdue) {
                if (o.getAssignedEmail() == null || o.getAssignedEmail().isBlank()) {
                    skipped++;
                    logger.warn("Obligation {} has no email — skipping overdue alert", o.getId());
                    continue;
                }
                sendOverdueAlert(o);
                sentOverdue++;
            }
            repository.markAlertsSent(ids);
            logger.info("Overdue alerts sent: {}", sentOverdue);
        }

        // ── Due-soon alerts ───────────────────────────────────────────────────
        if (!dueSoon.isEmpty()) {
            List<Long> ids = dueSoon.stream().map(ComplianceObligation::getId).collect(Collectors.toList());
            for (ComplianceObligation o : dueSoon) {
                if (o.getAssignedEmail() == null || o.getAssignedEmail().isBlank()) {
                    skipped++;
                    logger.warn("Obligation {} has no email — skipping due-soon alert", o.getId());
                    continue;
                }
                sendDueSoonAlert(o);
                sentDueSoon++;
            }
            repository.markAlertsSent(ids);
            logger.info("Due-soon alerts sent: {}", sentDueSoon);
        }

        return Map.of("overdue", sentOverdue, "dueSoon", sentDueSoon, "skipped", skipped);
    }

    // ── Weekly summary ────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklySummary() {
        logger.info("Sending weekly summary to {}", adminEmail);
        long totalPending = repository.countByStatus("PENDING");
        if (totalPending == 0) {
            logger.info("No pending obligations — skipping weekly summary");
            return;
        }
        List<ComplianceObligation> pending = repository.findByStatus("PENDING");
        StringBuilder body = new StringBuilder();
        body.append(String.format("Total pending obligations: %d%n%n", totalPending));
        for (ComplianceObligation o : pending) {
            body.append(String.format("- %s (Due: %s)%n", o.getTitle(), o.getDueDate()));
        }
        emailService.sendEmail(adminEmail, "Weekly Compliance Summary", body.toString());
        logger.info("Weekly summary sent with {} pending obligations", totalPending);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendOverdueAlert(ComplianceObligation o) {
        String body = String.format(
                "OVERDUE — Compliance Obligation Alert%n%n" +
                "Title:    %s%n" +
                "Due Date: %s%n" +
                "Status:   %s%n%n" +
                "This obligation is past its due date. Please take immediate action.",
                o.getTitle(), o.getDueDate(), o.getStatus());
        emailService.sendEmail(o.getAssignedEmail(), "⚠ Overdue: " + o.getTitle(), body);
        logger.info("Overdue alert sent for obligation {}", o.getId());
    }

    private void sendDueSoonAlert(ComplianceObligation o) {
        String body = String.format(
                "UPCOMING — Compliance Obligation Reminder%n%n" +
                "Title:    %s%n" +
                "Due Date: %s%n" +
                "Status:   %s%n%n" +
                "This obligation is due within 7 days. Please prepare accordingly.",
                o.getTitle(), o.getDueDate(), o.getStatus());
        emailService.sendEmail(o.getAssignedEmail(), "🔔 Due Soon: " + o.getTitle(), body);
        logger.info("Due-soon alert sent for obligation {}", o.getId());
    }
}
