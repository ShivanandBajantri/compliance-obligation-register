package com.internship.tool.controller;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.service.ComplianceObligationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obligations")
public class ComplianceObligationController {

    private final ComplianceObligationService service;

    public ComplianceObligationController(ComplianceObligationService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public ComplianceObligation update(@PathVariable Long id,
                                       @RequestBody ComplianceObligation updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        return service.delete(id);
    }

    @GetMapping("/status")
    public List<ComplianceObligation> getByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }

    @GetMapping("/stats")
    public long stats() {
        return service.count();
    }
}