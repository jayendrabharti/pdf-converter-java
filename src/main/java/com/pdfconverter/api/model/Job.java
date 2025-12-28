package com.pdfconverter.api.model;

import java.time.LocalDateTime;

/**
 * Represents a PDF conversion job.
 */
public class Job {
    private String jobId;
    private LocalDateTime createdAt;
    private int dpi;
    private String format;
    private String status;
    private String originalFilename;

    public Job(String jobId, int dpi, String format, String originalFilename) {
        this.jobId = jobId;
        this.dpi = dpi;
        this.format = format;
        this.originalFilename = originalFilename;
        this.createdAt = LocalDateTime.now();
        this.status = "processing";
    }

    // Getters and setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public boolean isExpired(int expiryHours) {
        return createdAt.plusHours(expiryHours).isBefore(LocalDateTime.now());
    }
}
