package com.internship.tool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ComplianceObligationDTO {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String status;
    private LocalDate dueDate;
    private String assignedEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public ComplianceObligationDTO() {}

    // Constructor for mapping from entity
    public ComplianceObligationDTO(Long id, String title, String description, String category,
                                 String status, LocalDate dueDate, String assignedEmail,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.dueDate = dueDate;
        this.assignedEmail = assignedEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssignedEmail() {
        return assignedEmail;
    }

    public void setAssignedEmail(String assignedEmail) {
        this.assignedEmail = assignedEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}