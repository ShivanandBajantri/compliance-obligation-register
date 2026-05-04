package com.internship.tool.service;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    public AlertScheduler(ComplianceObligationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    // Daily check for overdue and due soon obligations
    // Cron: "0 0 9 * * *" means every day at 9 AM
    // For testing, uncomment the @Scheduled(fixedRate = 60000) and comment the cron one
    // @Scheduled(fixedRate = 60000) // Every minute for testing
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkObligations() {
        logger.info("Starting daily obligation check");

        LocalDate today = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        // Optimized queries to avoid N+1 problem
        List<ComplianceObligation> overdueObligations = repository.findOverdueObligations(today);
        List<ComplianceObligation> dueSoonObligations = repository.findDueSoonObligations(dueSoonDate);

        // Process overdue obligations
        if (!overdueObligations.isEmpty()) {
            List<Long> overdueIds = overdueObligations.stream()
                    .map(ComplianceObligation::getId)
                    .collect(Collectors.toList());

            // Send alerts for overdue obligations
            overdueObligations.forEach(this::sendOverdueAlert);

            // Bulk update alert_sent flag
            repository.markAlertsSent(overdueIds);
            logger.info("Sent overdue alerts for {} obligations", overdueIds.size());
        }

        // Process due soon obligations
        if (!dueSoonObligations.isEmpty()) {
            List<Long> dueSoonIds = dueSoonObligations.stream()
                    .map(ComplianceObligation::getId)
                    .collect(Collectors.toList());

            // Send alerts for due soon obligations
            dueSoonObligations.forEach(this::sendDueSoonAlert);

            // Bulk update alert_sent flag
            repository.markAlertsSent(dueSoonIds);
            logger.info("Sent due soon alerts for {} obligations", dueSoonIds.size());
        }

        logger.info("Daily obligation check completed");
    }

    // Weekly summary every Monday at 9 AM
    // Cron: "0 0 9 * * MON"
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklySummary() {
        logger.info("Sending weekly summary");

        // Use optimized query instead of loading all entities
        long totalPending = repository.countByStatus("PENDING");

        if (totalPending > 0) {
            String subject = "Weekly Compliance Summary";
            String message = String.format("Total pending obligations: %d\n\n", totalPending);

            // Only load pending obligations with EntityGraph for better performance
            List<ComplianceObligation> pendingObligations = repository.findByStatus("PENDING");

            for (ComplianceObligation obligation : pendingObligations) {
                message += String.format("- %s (Due: %s)\n", obligation.getTitle(), obligation.getDueDate());
            }

            // Send to admin email - you can configure this in properties
            emailService.sendEmail("admin@company.com", subject, message);
            logger.info("Weekly summary sent with {} pending obligations", totalPending);
        } else {
            logger.info("No pending obligations for weekly summary");
        }
    }

    private void sendOverdueAlert(ComplianceObligation obligation) {
        String subject = "Overdue Compliance Alert";
        String message = String.format("The following compliance obligation is overdue:\n\n" +
                "Title: %s\n" +
                "Due Date: %s\n" +
                "Status: %s\n\n" +
                "Please take immediate action.",
                obligation.getTitle(), obligation.getDueDate(), obligation.getStatus());

        emailService.sendEmail(obligation.getAssignedEmail(), subject, message);
        logger.info("Overdue alert sent for obligation: {}", obligation.getId());
    }

    private void sendDueSoonAlert(ComplianceObligation obligation) {
        String subject = "Upcoming Compliance Alert";
        String message = String.format("The following compliance obligation is due in 7 days:\n\n" +
                "Title: %s\n" +
                "Due Date: %s\n" +
                "Status: %s\n\n" +
                "Please prepare accordingly.",
                obligation.getTitle(), obligation.getDueDate(), obligation.getStatus());

        emailService.sendEmail(obligation.getAssignedEmail(), subject, message);
        logger.info("Due soon alert sent for obligation: {}", obligation.getId());
    }
}