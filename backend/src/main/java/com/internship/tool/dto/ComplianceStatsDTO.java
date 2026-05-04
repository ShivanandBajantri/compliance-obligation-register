package com.internship.tool.dto;

public class ComplianceStatsDTO {

    private long totalObligations;
    private long pendingObligations;
    private long completedObligations;
    private long overdueObligations;
    private long dueSoonObligations;

    // Default constructor
    public ComplianceStatsDTO() {}

    // Constructor
    public ComplianceStatsDTO(long totalObligations, long pendingObligations,
                            long completedObligations, long overdueObligations,
                            long dueSoonObligations) {
        this.totalObligations = totalObligations;
        this.pendingObligations = pendingObligations;
        this.completedObligations = completedObligations;
        this.overdueObligations = overdueObligations;
        this.dueSoonObligations = dueSoonObligations;
    }

    // Getters and Setters
    public long getTotalObligations() {
        return totalObligations;
    }

    public void setTotalObligations(long totalObligations) {
        this.totalObligations = totalObligations;
    }

    public long getPendingObligations() {
        return pendingObligations;
    }

    public void setPendingObligations(long pendingObligations) {
        this.pendingObligations = pendingObligations;
    }

    public long getCompletedObligations() {
        return completedObligations;
    }

    public void setCompletedObligations(long completedObligations) {
        this.completedObligations = completedObligations;
    }

    public long getOverdueObligations() {
        return overdueObligations;
    }

    public void setOverdueObligations(long overdueObligations) {
        this.overdueObligations = overdueObligations;
    }

    public long getDueSoonObligations() {
        return dueSoonObligations;
    }

    public void setDueSoonObligations(long dueSoonObligations) {
        this.dueSoonObligations = dueSoonObligations;
    }
}