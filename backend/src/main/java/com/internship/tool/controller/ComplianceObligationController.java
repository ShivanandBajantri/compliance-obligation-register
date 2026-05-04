package com.internship.tool.controller;

import com.internship.tool.dto.ComplianceObligationDTO;
import com.internship.tool.dto.ComplianceStatsDTO;
import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.service.ComplianceObligationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/obligations")
public class ComplianceObligationController {

    private final ComplianceObligationService service;

    public ComplianceObligationController(ComplianceObligationService service) {
        this.service = service;
    }

    // ── Write endpoints ───────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation create(@RequestBody ComplianceObligation obligation) {
        return service.create(obligation);
    }

    /** Alias kept for backward compatibility. */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation createAlias(@RequestBody ComplianceObligation obligation) {
        return service.create(obligation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ComplianceObligation update(@PathVariable Long id,
                                       @RequestBody ComplianceObligation updated) {
        return service.update(id, updated);
    }

    /**
     * Delete an obligation.
     *
     * Bug fix: previously returned a plain String "Deleted" which is
     * inconsistent with all other endpoints that return JSON objects.
     * Now returns a JSON body so clients can parse the response uniformly.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Obligation " + id + " deleted successfully"));
    }

    // ── Read endpoints ────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ComplianceObligation getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Paginated list — full entity. Prefer /all-dto for list views. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligation> getAll(
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "10")  int    size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return service.getAll(buildPageable(page, size, sortBy, sortDir));
    }

    /** Paginated list — lightweight DTO, preferred for list views. */
    @GetMapping("/all-dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligationDTO> getAllAsDTO(
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "10")  int    size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return service.getAllAsDTO(buildPageable(page, size, sortBy, sortDir));
    }

    /** Filter by status — returns DTO list. */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public List<ComplianceObligationDTO> getByStatus(@RequestParam String status) {
        return service.getByStatusAsDTO(status);
    }

    /** Full-text keyword search with pagination — returns DTOs. */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligationDTO> search(
            @RequestParam(required = false)     String keyword,
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "10")  int    size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return service.search(keyword, buildPageable(page, size, sortBy, sortDir));
    }

    /** Dashboard statistics — single DB query, result is cached. */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ComplianceStatsDTO stats() {
        return service.getStats();
    }

    /**
     * CSV export — streams rows in pages of 500.
     *
     * Bug fix: original CSV only had 4 columns (ID, Title, Status, DueDate).
     * Added Category and AssignedEmail so the export is useful for demo.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=obligations.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Title,Category,Status,DueDate,AssignedEmail");

            int page = 0;
            final int PAGE_SIZE = 500;
            Page<ComplianceObligation> chunk;

            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("id"));
                chunk = service.getAll(pageable);
                for (ComplianceObligation o : chunk.getContent()) {
                    writer.println(String.join(",",
                            escapeCsv(o.getId()),
                            escapeCsv(o.getTitle()),
                            escapeCsv(o.getCategory()),
                            escapeCsv(o.getStatus()),
                            escapeCsv(o.getDueDate()),
                            escapeCsv(o.getAssignedEmail())));
                }
            } while (chunk.hasNext());

            writer.flush();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String text = value.toString().replace("\"", "\"\"");
        return (text.contains(",") || text.contains("\"") || text.contains("\n"))
                ? "\"" + text + "\""
                : text;
    }
}
