package com.internship.tool.controller;

import com.internship.tool.dto.ComplianceObligationDTO;
import com.internship.tool.dto.ComplianceStatsDTO;
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

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        return service.delete(id);
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ComplianceObligation getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Paginated list returning full entities.
     * Prefer /all-dto for read-heavy clients — it transfers fewer bytes.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligation> getAll(
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "10")   int    size,
            @RequestParam(defaultValue = "id")   String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return service.getAll(pageable);
    }

    /**
     * Paginated list returning lightweight DTOs — preferred for list views.
     */
    @GetMapping("/all-dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligationDTO> getAllAsDTO(
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "10")   int    size,
            @RequestParam(defaultValue = "id")   String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return service.getAllAsDTO(pageable);
    }

    /**
     * Filter by status — returns DTO list.
     * Previously there were two conflicting @GetMapping("/search") methods on
     * this class; one has been removed and the status filter is now at /status.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public List<ComplianceObligationDTO> getByStatus(@RequestParam String status) {
        return service.getByStatusAsDTO(status);
    }

    /**
     * Full-text keyword search with pagination.
     * Returns DTOs to minimise payload size.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public Page<ComplianceObligationDTO> search(
            @RequestParam(required = false)      String keyword,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "10")   int    size,
            @RequestParam(defaultValue = "id")   String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return service.search(keyword, pageable);
    }

    /**
     * Dashboard statistics — single DB query, result is cached.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ComplianceStatsDTO stats() {
        return service.getStats();
    }

    /**
     * CSV export.
     *
     * Streams rows in pages of 500 to avoid loading the entire table into the
     * JVM heap.  For very large datasets consider a database-side COPY command
     * or a dedicated reporting service.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=obligations.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Title,Status,DueDate");

            int page = 0;
            final int PAGE_SIZE = 500;
            Page<ComplianceObligation> chunk;

            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("id"));
                chunk = service.getAll(pageable);
                for (ComplianceObligation o : chunk.getContent()) {
                    writer.println(String.format("%s,%s,%s,%s",
                            escapeCsv(o.getId()),
                            escapeCsv(o.getTitle()),
                            escapeCsv(o.getStatus()),
                            escapeCsv(o.getDueDate())));
                }
            } while (chunk.hasNext());

            writer.flush();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
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
}
