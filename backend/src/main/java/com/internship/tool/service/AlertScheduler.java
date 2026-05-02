package com.internship.tool.service;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

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
    public void checkObligations() {
        logger.info("Starting daily obligation check");

        LocalDate today = LocalDate.now();
        LocalDate dueSoonDate = today.plusDays(7);

        // Fetch all obligations
        List<ComplianceObligation> obligations = repository.findAll();

        for (ComplianceObligation obligation : obligations) {
            if (obligation.getAssignedEmail() == null || obligation.getAssignedEmail().isEmpty()) {
                continue; // Skip if no email assigned
            }

            boolean isOverdue = obligation.getDueDate().isBefore(today) && !"COMPLETED".equals(obligation.getStatus());
            boolean isDueSoon = obligation.getDueDate().isEqual(dueSoonDate);

            if (isOverdue && !obligation.isAlertSent()) {
                sendOverdueAlert(obligation);
                obligation.setAlertSent(true);
                repository.save(obligation);
            } else if (isDueSoon && !obligation.isAlertSent()) {
                sendDueSoonAlert(obligation);
                obligation.setAlertSent(true);
                repository.save(obligation);
            }
        }

        logger.info("Daily obligation check completed");
    }

    // Weekly summary every Monday at 9 AM
    // Cron: "0 0 9 * * MON"
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklySummary() {
        logger.info("Sending weekly summary");

        List<ComplianceObligation> pendingObligations = repository.findByStatus("PENDING");
        long totalPending = pendingObligations.size();

        String subject = "Weekly Compliance Summary";
        String message = String.format("Total pending obligations: %d\n\n", totalPending);

        for (ComplianceObligation obligation : pendingObligations) {
            message += String.format("- %s (Due: %s)\n", obligation.getTitle(), obligation.getDueDate());
        }

        // Send to admin email - you can configure this in properties
        emailService.sendEmail("admin@company.com", subject, message);

        logger.info("Weekly summary sent");
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