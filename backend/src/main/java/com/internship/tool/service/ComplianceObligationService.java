package com.internship.tool.service;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplianceObligationService {

    private final ComplianceObligationRepository repository;

    public ComplianceObligationService(ComplianceObligationRepository repository) {
        this.repository = repository;
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