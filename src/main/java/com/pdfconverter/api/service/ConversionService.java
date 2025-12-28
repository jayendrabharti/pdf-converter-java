package com.pdfconverter.api.service;

import com.pdfconverter.api.model.Job;
import com.pdfconverter.core.PdfConverter;
import com.pdfconverter.core.PdfRepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Service for handling PDF to Image conversion.
 */
@Service
public class ConversionService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JobManager jobManager;

    @Autowired
    private PdfRepairService pdfRepairService;

    /**
     * Converts a PDF file to images.
     *
     * @param inputPdf Input PDF file
     * @param job      Job information
     * @return Metadata map
     * @throws IOException if conversion fails
     */
    public Map<String, Object> convertPdf(File inputPdf, Job job) throws IOException {
        // Create output directory
        File outputDir = fileStorageService.createOutputDirectory(job.getJobId());

        // Perform conversion with repair service
        PdfConverter converter = new PdfConverter(pdfRepairService);
        Map<String, Object> metadata = converter.convertForApi(
                inputPdf,
                outputDir,
                job.getDpi(),
                job.getFormat()
        );

        // Update job status
        jobManager.updateJobStatus(job.getJobId(), "completed");

        return metadata;
    }
}
