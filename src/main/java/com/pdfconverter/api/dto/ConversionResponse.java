package com.pdfconverter.api.dto;

import java.util.Map;

/**
 * Response DTO for PDF conversion requests.
 */
public class ConversionResponse {
    private String jobId;
    private String status;
    private Map<String, Object> metadata;
    private String downloadUrl;

    public ConversionResponse() {
    }

    public ConversionResponse(String jobId, String status, Map<String, Object> metadata, String downloadUrl) {
        this.jobId = jobId;
        this.status = status;
        this.metadata = metadata;
        this.downloadUrl = downloadUrl;
    }

    // Getters and setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
