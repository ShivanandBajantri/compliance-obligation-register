package com.internship.tool;

import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("compliance_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ComplianceObligationRepository repository;

    @Test
    public void testFullCrudOperations() {
        // CREATE: Create and save a new compliance obligation
        ComplianceObligation obligation = new ComplianceObligation();
        obligation.setTitle("Test Compliance Obligation");
        obligation.setDescription("This is a test obligation for integration testing");
        obligation.setCategory("Testing");
        obligation.setStatus("PENDING");
        obligation.setDueDate(LocalDate.now().plusDays(30));
        obligation.setAssignedEmail("test@example.com");
        obligation.setAlertSent(false);

        ComplianceObligation savedObligation = repository.save(obligation);
        assertNotNull(savedObligation.getId(), "ID should be generated after save");

        // READ: Fetch the saved entity by ID and verify all fields
        Optional<ComplianceObligation> fetchedObligation = repository.findById(savedObligation.getId());
        assertTrue(fetchedObligation.isPresent(), "Entity should be found by ID");

        ComplianceObligation retrieved = fetchedObligation.get();
        assertEquals("Test Compliance Obligation", retrieved.getTitle());
        assertEquals("This is a test obligation for integration testing", retrieved.getDescription());
        assertEquals("Testing", retrieved.getCategory());
        assertEquals("PENDING", retrieved.getStatus());
        assertEquals(LocalDate.now().plusDays(30), retrieved.getDueDate());
        assertEquals("test@example.com", retrieved.getAssignedEmail());
        assertFalse(retrieved.isAlertSent());

        // UPDATE: Modify fields and save again
        retrieved.setStatus("IN_PROGRESS");
        retrieved.setAlertSent(true);
        ComplianceObligation updatedObligation = repository.save(retrieved);

        // Verify the update
        Optional<ComplianceObligation> updatedFetched = repository.findById(updatedObligation.getId());
        assertTrue(updatedFetched.isPresent());
        assertEquals("IN_PROGRESS", updatedFetched.get().getStatus());
        assertTrue(updatedFetched.get().isAlertSent());

        // DELETE: Remove the entity and verify deletion
        repository.deleteById(savedObligation.getId());

        // Verify deletion
        Optional<ComplianceObligation> deletedObligation = repository.findById(savedObligation.getId());
        assertFalse(deletedObligation.isPresent(), "Entity should be deleted");
    }
}