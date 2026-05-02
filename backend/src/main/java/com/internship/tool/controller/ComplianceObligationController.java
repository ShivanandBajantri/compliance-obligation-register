package com.internship.tool.controller;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.service.ComplianceObligationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obligations")
public class ComplianceObligationController {

    private final ComplianceObligationService service;

    public ComplianceObligationController(ComplianceObligationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation create(@RequestBody ComplianceObligation obligation) {
        return service.create(obligation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation update(@PathVariable Long id,
                                       @RequestBody ComplianceObligation updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        return service.delete(id);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public List<ComplianceObligation> getByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public long stats() {
        return service.count();
    }
}