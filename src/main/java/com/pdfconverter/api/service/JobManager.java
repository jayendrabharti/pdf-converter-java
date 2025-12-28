package com.pdfconverter.api.service;

import com.pdfconverter.api.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversion jobs.
 * Handles job creation, tracking, and cleanup.
 */
@Service
public class JobManager {

    @Value("${app.job.expiry-hours:1}")
    private int expiryHours;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirConfig;

    @Value("${app.output.dir:outputs}")
    private String outputDirConfig;

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    /**
     * Gets absolute path for upload directory.
     */
    private String getUploadDir() {
        String workingDir = System.getProperty("user.dir");
        return new File(workingDir, uploadDirConfig).getAbsolutePath();
    }

    /**
     * Gets absolute path for output directory.
     */
    private String getOutputDir() {
        String workingDir = System.getProperty("user.dir");
        return new File(workingDir, outputDirConfig).getAbsolutePath();
    }

    /**
     * Creates a new job with a unique ID.
     *
     * @param dpi             DPI setting
     * @param format          Image format
     * @param originalFilename Original PDF filename
     * @return Created job
     */
    public Job createJob(int dpi, String format, String originalFilename) {
        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, dpi, format, originalFilename);
        jobs.put(jobId, job);
        return job;
    }

    /**
     * Gets a job by ID.
     *
     * @param jobId Job ID
     * @return Job if found, null otherwise
     */
    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * Checks if a job exists.
     *
     * @param jobId Job ID
     * @return true if job exists
     */
    public boolean jobExists(String jobId) {
        return jobs.containsKey(jobId);
    }

    /**
     * Updates job status.
     *
     * @param jobId  Job ID
     * @param status New status
     */
    public void updateJobStatus(String jobId, String status) {
        Job job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(status);
        }
    }

    /**
     * Deletes job files and removes from tracking.
     *
     * @param jobId Job ID
     * @throws IOException if deletion fails
     */
    public void deleteJob(String jobId) throws IOException {
        // Remove from map
        jobs.remove(jobId);

        // Delete upload directory
        File uploadPath = new File(getUploadDir(), jobId);
        if (uploadPath.exists()) {
            FileUtils.deleteDirectory(uploadPath);
        }

        // Delete output directory
        File outputPath = new File(getOutputDir(), jobId);
        if (outputPath.exists()) {
            FileUtils.deleteDirectory(outputPath);
        }
    }

    /**
     * Scheduled task to cleanup expired jobs.
     * Runs every 15 minutes by default.
     */
    @Scheduled(cron = "${app.cleanup.cron:0 */15 * * * *}")
    public void cleanupExpiredJobs() {
        int cleanedCount = 0;
        
        for (Map.Entry<String, Job> entry : jobs.entrySet()) {
            Job job = entry.getValue();
            if (job.isExpired(expiryHours)) {
                try {
                    deleteJob(job.getJobId());
                    cleanedCount++;
                } catch (IOException e) {
                    System.err.println("Failed to cleanup job " + job.getJobId() + ": " + e.getMessage());
                }
            }
        }

        if (cleanedCount > 0) {
            System.out.println("Cleaned up " + cleanedCount + " expired job(s)");
        }
    }

    /**
     * Gets total number of active jobs.
     *
     * @return Number of jobs
     */
    public int getJobCount() {
        return jobs.size();
    }
}
