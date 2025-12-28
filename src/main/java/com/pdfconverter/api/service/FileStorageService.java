package com.pdfconverter.api.service;

import com.pdfconverter.util.ZipUtility;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Service for handling file storage operations.
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDirConfig;

    @Value("${app.output.dir:outputs}")
    private String outputDirConfig;

    /**
     * Gets the absolute path for upload directory.
     */
    private String getUploadDir() {
        String workingDir = System.getProperty("user.dir");
        String path = new File(workingDir, uploadDirConfig).getAbsolutePath();
        // Ensure directory exists
        try {
            Files.createDirectories(new File(path).toPath());
        } catch (IOException e) {
            // Directory creation will be handled when actually saving files
        }
        return path;
    }

    /**
     * Gets the absolute path for output directory.
     */
    private String getOutputDir() {
        String workingDir = System.getProperty("user.dir");
        String path = new File(workingDir, outputDirConfig).getAbsolutePath();
        // Ensure directory exists
        try {
            Files.createDirectories(new File(path).toPath());
        } catch (IOException e) {
            // Directory creation will be handled when actually saving files
        }
        return path;
    }

    /**
     * Saves an uploaded PDF file.
     *
     * @param file  Multipart file
     * @param jobId Job ID
     * @return Saved file
     * @throws IOException if saving fails
     */
    public File saveUploadedFile(MultipartFile file, String jobId) throws IOException {
        // Create upload directory for this job
        File jobUploadDir = new File(getUploadDir(), jobId);
        if (!jobUploadDir.exists()) {
            Files.createDirectories(jobUploadDir.toPath());
        }

        // Save the PDF
        File pdfFile = new File(jobUploadDir, "input.pdf");
        file.transferTo(pdfFile);

        return pdfFile;
    }

    /**
     * Creates output directory for a job.
     *
     * @param jobId Job ID
     * @return Output directory
     * @throws IOException if creation fails
     */
    public File createOutputDirectory(String jobId) throws IOException {
        File jobOutputDir = new File(getOutputDir(), jobId);
        if (!jobOutputDir.exists()) {
            Files.createDirectories(jobOutputDir.toPath());
        }
        return jobOutputDir;
    }

    /**
     * Gets the output directory for a job.
     *
     * @param jobId Job ID
     * @return Output directory
     */
    public File getOutputDirectory(String jobId) {
        return new File(getOutputDir(), jobId);
    }

    /**
     * Creates a ZIP archive of the output files.
     *
     * @param jobId Job ID
     * @return ZIP file
     * @throws IOException if zipping fails
     */
    public File zipOutputFiles(String jobId) throws IOException {
        File outputDirFile = getOutputDirectory(jobId);
        File zipFile = new File(getOutputDir(), jobId + ".zip");

        ZipUtility.zipDirectory(outputDirFile, zipFile);

        return zipFile;
    }

    /**
     * Checks if output files exist for a job.
     *
     * @param jobId Job ID
     * @return true if output directory exists and is not empty
     */
    public boolean outputExists(String jobId) {
        File outputDirFile = getOutputDirectory(jobId);
        if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
            return false;
        }

        File[] files = outputDirFile.listFiles();
        return files != null && files.length > 0;
    }

    /**
     * Deletes all files for a job.
     *
     * @param jobId Job ID
     * @throws IOException if deletion fails
     */
    public void deleteJobFiles(String jobId) throws IOException {
        // Delete upload directory
        File uploadDirFile = new File(getUploadDir(), jobId);
        if (uploadDirFile.exists()) {
            FileUtils.deleteDirectory(uploadDirFile);
        }

        // Delete output directory
        File outputDirFile = new File(getOutputDir(), jobId);
        if (outputDirFile.exists()) {
            FileUtils.deleteDirectory(outputDirFile);
        }

        // Delete ZIP file if exists
        File zipFile = new File(getOutputDir(), jobId + ".zip");
        if (zipFile.exists()) {
            Files.delete(zipFile.toPath());
        }
    }
}
