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

    // ── Scheduled daily trigger (automatic) ──────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkObligations() {
        logger.info("Scheduled daily obligation check started");
        LocalDate today       = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        List<ComplianceObligation> overdue = repository.findOverdueObligations(today);
        List<ComplianceObligation> dueSoon = repository.findDueSoonObligations(today, dueSoonDate);

        if (!overdue.isEmpty()) {
            overdue.stream()
                .filter(o -> o.getAssignedEmail() != null && !o.getAssignedEmail().isBlank())
                .forEach(this::sendOverdueAlert);
            repository.markAlertsSent(
                overdue.stream().map(ComplianceObligation::getId).collect(Collectors.toList()));
        }
        if (!dueSoon.isEmpty()) {
            dueSoon.stream()
                .filter(o -> o.getAssignedEmail() != null && !o.getAssignedEmail().isBlank())
                .forEach(this::sendDueSoonAlert);
            repository.markAlertsSent(
                dueSoon.stream().map(ComplianceObligation::getId).collect(Collectors.toList()));
        }
        logger.info("Scheduled check done — overdue={} dueSoon={}", overdue.size(), dueSoon.size());
    }

    // ── Manual trigger — called from the "Send Alerts" button ────────────────

    /**
     * Sends alert emails only to OVERDUE and DUE-SOON obligations.
     * - Overdue  = dueDate is in the past and status != COMPLETED
     * - Due soon = dueDate is within the next 7 days and status != COMPLETED
     *
     * The manual button ignores the alertSent flag so it always sends
     * when clicked. The alertSent flag is only used by the automatic
     * daily scheduler to avoid duplicate emails.
     *
     * Emails go to o.getAssignedEmail() — NOT to the .env sender account.
     */
    @Transactional
    public Map<String, Object> sendAlerts() {
        LocalDate today       = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        // Use queries that ignore alertSent — manual button always sends
        List<ComplianceObligation> overdue = repository.findAll().stream()
                .filter(o -> o.getDueDate() != null
                        && o.getDueDate().isBefore(today)
                        && !"COMPLETED".equals(o.getStatus())
                        && !"CANCELLED".equals(o.getStatus()))
                .collect(Collectors.toList());

        List<ComplianceObligation> dueSoon = repository.findAll().stream()
                .filter(o -> o.getDueDate() != null
                        && !o.getDueDate().isBefore(today)
                        && !o.getDueDate().isAfter(dueSoonDate)
                        && !"COMPLETED".equals(o.getStatus())
                        && !"CANCELLED".equals(o.getStatus()))
                .collect(Collectors.toList());

        int sentOverdue = 0, sentDueSoon = 0, skipped = 0;

        for (ComplianceObligation o : overdue) {
            if (o.getAssignedEmail() == null || o.getAssignedEmail().isBlank()) {
                skipped++;
                continue;
            }
            sendObligationAlert(o);
            sentOverdue++;
        }

        for (ComplianceObligation o : dueSoon) {
            if (o.getAssignedEmail() == null || o.getAssignedEmail().isBlank()) {
                skipped++;
                continue;
            }
            sendObligationAlert(o);
            sentDueSoon++;
        }

        int total = sentOverdue + sentDueSoon;
        String message = total == 0
                ? "No overdue or due-soon obligations found — no alerts sent."
                : String.format("Alerts sent: %d overdue, %d due-soon.%s",
                        sentOverdue, sentDueSoon,
                        skipped > 0 ? " " + skipped + " skipped (no email assigned)." : "");

        logger.info("Manual alert — overdue={} dueSoon={} skipped={}", sentOverdue, sentDueSoon, skipped);
        return Map.of("overdue", sentOverdue, "dueSoon", sentDueSoon,
                      "skipped", skipped, "total", total, "message", message);
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

    // ── Email helpers ─────────────────────────────────────────────────────────

    /**
     * Sends a status-aware alert to o.getAssignedEmail().
     * Subject and body vary based on whether the obligation is overdue,
     * due soon, or just a general reminder.
     */
    private void sendObligationAlert(ComplianceObligation o) {
        LocalDate today = LocalDate.now();
        LocalDate due   = o.getDueDate();

        String subject;
        String body;

        if (due != null && due.isBefore(today)) {
            // Overdue
            subject = "⚠ OVERDUE: " + o.getTitle();
            body = String.format(
                    "Compliance Obligation Alert — OVERDUE%n%n" +
                    "Title:        %s%n" +
                    "Category:     %s%n" +
                    "Due Date:     %s%n" +
                    "Status:       %s%n" +
                    "Assigned To:  %s%n%n" +
                    "This obligation is past its due date. Please take immediate action.",
                    o.getTitle(), o.getCategory(), o.getDueDate(),
                    o.getStatus(), o.getAssignedEmail());

        } else if (due != null && !due.isAfter(today.plusDays(7))) {
            // Due within 7 days
            subject = "🔔 Due Soon: " + o.getTitle();
            body = String.format(
                    "Compliance Obligation Reminder — DUE SOON%n%n" +
                    "Title:        %s%n" +
                    "Category:     %s%n" +
                    "Due Date:     %s%n" +
                    "Status:       %s%n" +
                    "Assigned To:  %s%n%n" +
                    "This obligation is due within 7 days. Please prepare accordingly.",
                    o.getTitle(), o.getCategory(), o.getDueDate(),
                    o.getStatus(), o.getAssignedEmail());

        } else {
            // General reminder
            subject = "📋 Compliance Reminder: " + o.getTitle();
            body = String.format(
                    "Compliance Obligation Reminder%n%n" +
                    "Title:        %s%n" +
                    "Category:     %s%n" +
                    "Due Date:     %s%n" +
                    "Status:       %s%n" +
                    "Assigned To:  %s%n%n" +
                    "Please review and ensure this obligation is completed on time.",
                    o.getTitle(), o.getCategory(), o.getDueDate(),
                    o.getStatus(), o.getAssignedEmail());
        }

        // Send to the obligation's assigned email — NOT to the .env sender account
        emailService.sendEmail(o.getAssignedEmail(), subject, body);
        logger.info("Alert sent to {} for obligation '{}' (id={})",
                o.getAssignedEmail(), o.getTitle(), o.getId());
    }

    private void sendOverdueAlert(ComplianceObligation o) {
        sendObligationAlert(o);
    }

    private void sendDueSoonAlert(ComplianceObligation o) {
        sendObligationAlert(o);
    }
}
