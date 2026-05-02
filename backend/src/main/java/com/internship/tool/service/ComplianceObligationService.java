package com.internship.tool.service;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplianceObligationService {

    private final ComplianceObligationRepository repository;
    private final EmailService emailService;

    public ComplianceObligationService(ComplianceObligationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public ComplianceObligation create(ComplianceObligation obligation) {
        obligation.setCreatedAt(LocalDateTime.now());
        obligation.setUpdatedAt(LocalDateTime.now());
        obligation.setStatus("PENDING");
        obligation.setAlertSent(false);

        ComplianceObligation saved = repository.save(obligation);

        // Send email notification
        if (obligation.getAssignedEmail() != null && !obligation.getAssignedEmail().isEmpty()) {
            String subject = "New Obligation Assigned";
            String message = String.format("A new compliance obligation has been assigned to you:\n\n" +
                    "Title: %s\n" +
                    "Due Date: %s\n\n" +
                    "Please review and complete as required.",
                    obligation.getTitle(), obligation.getDueDate());

            emailService.sendEmail(obligation.getAssignedEmail(), subject, message);
        }

        return saved;
    }

    public ComplianceObligation update(Long id, ComplianceObligation updated) {
        ComplianceObligation existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        existing.setStatus(updated.getStatus());
        existing.setDueDate(updated.getDueDate());

        return repository.save(existing);
    }

    public String delete(Long id) {
        ComplianceObligation existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        existing.setStatus("DELETED");
        repository.save(existing);

        return "Deleted";
    }

    public List<ComplianceObligation> getByStatus(String status) {
        return repository.findByStatus(status);
    }

    public long count() {
        return repository.count();
    }
}