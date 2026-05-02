package com.internship.tool.controller;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.service.ComplianceObligationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation createAlias(@RequestBody ComplianceObligation obligation) {
        return service.create(obligation);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ComplianceObligation getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public List<ComplianceObligation> searchByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligation> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return service.getAll(pageable);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public void exportCsv(HttpServletResponse response) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=obligations.csv");

        List<ComplianceObligation> obligations = service.getAll();

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Title,Status,DueDate");
            for (ComplianceObligation obligation : obligations) {
                writer.println(String.format(
                        "%s,%s,%s,%s",
                        escapeCsv(obligation.getId()),
                        escapeCsv(obligation.getTitle()),
                        escapeCsv(obligation.getStatus()),
                        escapeCsv(obligation.getDueDate())
                ));
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export obligations CSV", e);
        }
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text + "\"";
        }
        return text;
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