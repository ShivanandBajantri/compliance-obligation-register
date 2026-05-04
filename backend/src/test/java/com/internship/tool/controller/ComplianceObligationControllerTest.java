package com.internship.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.service.ComplianceObligationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ComplianceObligationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceObligationService service;

    private ComplianceObligation testObligation;

    @BeforeEach
    public void setup() {
        testObligation = new ComplianceObligation();
        testObligation.setTitle("Test Obligation");
        testObligation.setDescription("This is a test obligation");
        testObligation.setCategory("COMPLIANCE");
        testObligation.setStatus("PENDING");
        testObligation.setDueDate(LocalDate.now().plusDays(30));
        testObligation.setAssignedEmail("test@example.com");
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateObligation_Success() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Obligation"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateObligationAlias_Success() throws Exception {
        mockMvc.perform(post("/api/obligations/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Obligation"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testCreateObligation_Forbidden() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateObligation_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isForbidden()); // anonymous → 403
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateObligation_InvalidJson() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetAll_Success() throws Exception {
        mockMvc.perform(get("/api/obligations/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetAll_WithPagination() throws Exception {
        mockMvc.perform(get("/api/obligations/all")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(5));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetAll_WithDescendingSort() throws Exception {
        mockMvc.perform(get("/api/obligations/all")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "dueDate")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetById_Success() throws Exception {
        ComplianceObligation created = service.create(testObligation);

        mockMvc.perform(get("/api/obligations/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.title").value("Test Obligation"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetById_NotFound() throws Exception {
        // GlobalExceptionHandler maps "not found" RuntimeException → 404
        mockMvc.perform(get("/api/obligations/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetByStatus_Success() throws Exception {
        service.create(testObligation);

        mockMvc.perform(get("/api/obligations/status").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testSearch_Success() throws Exception {
        service.create(testObligation);

        mockMvc.perform(get("/api/obligations/search").param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testSearch_EmptyKeyword_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/obligations/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testGetStats_Success() throws Exception {
        mockMvc.perform(get("/api/obligations/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalObligations").isNumber())
                .andExpect(jsonPath("$.pendingObligations").isNumber())
                .andExpect(jsonPath("$.completedObligations").isNumber())
                .andExpect(jsonPath("$.overdueObligations").isNumber())
                .andExpect(jsonPath("$.dueSoonObligations").isNumber());
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateObligation_Success() throws Exception {
        ComplianceObligation created = service.create(testObligation);

        testObligation.setStatus("COMPLETED");
        testObligation.setDueDate(LocalDate.now().plusDays(45));

        mockMvc.perform(put("/api/obligations/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateObligation_NotFound() throws Exception {
        testObligation.setStatus("COMPLETED");

        // GlobalExceptionHandler maps "not found" RuntimeException → 404
        mockMvc.perform(put("/api/obligations/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testUpdateObligation_Forbidden() throws Exception {
        ComplianceObligation created = service.create(testObligation);

        mockMvc.perform(put("/api/obligations/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testObligation)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteObligation_Success() throws Exception {
        ComplianceObligation created = service.create(testObligation);

        mockMvc.perform(delete("/api/obligations/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteObligation_NotFound() throws Exception {
        // GlobalExceptionHandler maps "not found" RuntimeException → 404
        mockMvc.perform(delete("/api/obligations/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void testDeleteObligation_Forbidden() throws Exception {
        ComplianceObligation created = service.create(testObligation);

        mockMvc.perform(delete("/api/obligations/{id}", created.getId()))
                .andExpect(status().isForbidden());
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testExportCsv_Success() throws Exception {
        service.create(testObligation);

        mockMvc.perform(get("/api/obligations/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("ID,Title,Category,Status,DueDate,AssignedEmail")));
    }
}
