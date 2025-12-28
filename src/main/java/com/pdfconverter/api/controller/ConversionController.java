package com.pdfconverter.api.controller;

import com.pdfconverter.api.dto.ApiInfo;
import com.pdfconverter.api.dto.ConversionResponse;
import com.pdfconverter.api.model.Job;
import com.pdfconverter.api.service.ConversionService;
import com.pdfconverter.api.service.FileStorageService;
import com.pdfconverter.api.service.JobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for PDF conversion endpoints.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ConversionController {

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JobManager jobManager;

    /**
     * POST /api/convert - Upload and convert PDF to images
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> convertPdf(
            @RequestParam("pdf") MultipartFile pdfFile,
            @RequestParam(value = "dpi", defaultValue = "150") int dpi,
            @RequestParam(value = "format", defaultValue = "jpg") String format) {

        try {
            // Validate inputs
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "PDF file is required"));
            }

            String filename = pdfFile.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be a PDF"));
            }

            if (dpi < 50 || dpi > 600) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "DPI must be between 50 and 600"));
            }

            if (!format.equalsIgnoreCase("jpg") && !format.equalsIgnoreCase("png")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Format must be 'jpg' or 'png'"));
            }

            // Create job
            Job job = jobManager.createJob(dpi, format.toLowerCase(), filename);

            // Save uploaded file
            File savedPdf = fileStorageService.saveUploadedFile(pdfFile, job.getJobId());

            // Convert PDF
            Map<String, Object> metadata = conversionService.convertPdf(savedPdf, job);

            // Build response
            ConversionResponse response = new ConversionResponse();
            response.setJobId(job.getJobId());
            response.setStatus("success");
            response.setMetadata(metadata);
            response.setDownloadUrl("/api/output/" + job.getJobId());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Conversion failed",
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Unexpected error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * GET /api/output/{jobId} - Download converted images as ZIP
     */
    @GetMapping("/output/{jobId}")
    public ResponseEntity<?> downloadOutput(@PathVariable String jobId) {
        try {
            // Check if job exists
            if (!jobManager.jobExists(jobId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Job not found or expired"));
            }

            // Check if output exists
            if (!fileStorageService.outputExists(jobId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Output files not found"));
            }

            // Create ZIP file
            File zipFile = fileStorageService.zipOutputFiles(jobId);

            // Return ZIP as download
            Resource resource = new FileSystemResource(zipFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + jobId + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.length())
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to create download",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * GET /api/help - API documentation
     */
    @GetMapping("/help")
    public ResponseEntity<ApiInfo> getHelp() {
        Map<String, ApiInfo.EndpointInfo> endpoints = new HashMap<>();

        endpoints.put("POST /api/convert", new ApiInfo.EndpointInfo(
                "Convert PDF to images",
                Map.of(
                        "pdf", "PDF file (multipart/form-data)",
                        "dpi", "Resolution (50-600, default: 150)",
                        "format", "Output format (jpg|png, default: jpg)"
                )
        ));

        endpoints.put("GET /api/output/:jobId", new ApiInfo.EndpointInfo(
                "Download converted images as ZIP",
                Map.of("jobId", "Job ID from conversion response")
        ));

        endpoints.put("GET /api/help", new ApiInfo.EndpointInfo(
                "API documentation",
                Map.of()
        ));

        endpoints.put("GET /health", new ApiInfo.EndpointInfo(
                "Health check endpoint",
                Map.of()
        ));

        ApiInfo info = new ApiInfo("PDF to Image Converter API", "1.0.0", endpoints);
        return ResponseEntity.ok(info);
    }

    /**
     * GET /health - Health check
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("activeJobs", jobManager.getJobCount());
        health.put("api", "running");

        return ResponseEntity.ok(health);
    }
}
