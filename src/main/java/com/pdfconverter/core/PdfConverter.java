package com.pdfconverter.core;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core PDF to Image conversion engine using Apache PDFBox.
 * Optimized with ForkJoinPool for better performance and PDDocument reuse.
 * Integrates PDF repair for handling problematic PDFs.
 */
@Component
public class PdfConverter {
    private final ImageWriter imageWriter;
    private final MetadataGenerator metadataGenerator;
    private final PdfRepairService repairService;
    private final PdfValidationService validationService;
    
    @Value("${app.conversion.min-threads:2}")
    private int minThreads;
    
    @Value("${app.conversion.max-threads:8}")
    private int maxThreads;
    
    @Value("${app.conversion.pages-per-thread:50}")
    private int pagesPerThread;
    
    @Value("${app.repair.skip-threshold-percent:5.0}")
    private double repairSkipThreshold;

    public PdfConverter(ImageWriter imageWriter, 
                       MetadataGenerator metadataGenerator,
                       PdfRepairService repairService,
                       PdfValidationService validationService) {
        this.imageWriter = imageWriter;
        this.metadataGenerator = metadataGenerator;
        this.repairService = repairService;
        this.validationService = validationService;
    }

    /**
     * Converts a PDF file to images for API use.
     * Automatically attempts repair if conversion fails.
     *
     * @param inputPdf  Input PDF file
     * @param outputDir Output directory
     * @param dpi       DPI setting
     * @param format    Image format (jpg/png)
     * @return Metadata map with conversion details
     * @throws IOException if conversion fails
     */
    public Map<String, Object> convertForApi(File inputPdf, File outputDir, int dpi, String format) throws IOException {
        long overallStartTime = System.currentTimeMillis();
        
        // OPTIMIZATION: Quick validation before loading PDF
        PdfValidationService.ValidationResult validation = validationService.validatePdf(inputPdf);
        if (!validation.isValid() && !validation.hasStructureIssues()) {
            throw new IOException("Invalid PDF: " + validation.getError());
        }
        
        if (validation.isEncrypted()) {
            System.out.println("⚠ PDF is encrypted - attempting to remove security");
        }
        
        // OPTIMIZATION: Load PDF once and reuse for all attempts
        PDDocument document = PDDocument.load(inputPdf, 
            org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly());
        document.setAllSecurityToBeRemoved(true);
        
        try {
            // Attempt 1: Direct conversion at requested DPI
            Map<String, Object> result = attemptConversionWithDocument(document, outputDir, dpi, format, inputPdf.getName());
            int failedCount = (Integer) result.get("failedPages");
            int totalPages = (Integer) result.get("totalPages");
            
            // OPTIMIZATION: Skip repair if failure rate is below threshold
            double failurePercentage = (failedCount * 100.0) / totalPages;
            if (failedCount > 0 && failurePercentage < repairSkipThreshold) {
                System.out.println(String.format(
                    "⚡ Skipping repair - only %.1f%% failed (threshold: %.1f%%)",
                    failurePercentage, repairSkipThreshold));
                result.put("totalTimeSeconds", (System.currentTimeMillis() - overallStartTime) / 1000.0);
                return result;
            }
        
        // If there are failures and repair is available, try repair strategies
        if (failedCount > 0 && repairService != null && repairService.isAnyRepairAvailable()) {
            System.out.println("\n⚠ " + failedCount + " page(s) failed. Attempting PDF repair...");
            
            // Extract failed page numbers from errors
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            List<Integer> failedPageNumbers = extractPageNumbers(errors);
            
            File repairedPdf = null;
            
            // Priority 2: Try QPDF repair (fast, preserves quality)
            if (repairService.isQpdfAvailable()) {
                try {
                    System.out.println("→ Strategy 2: QPDF repair...");
                    repairedPdf = repairService.repairWithQpdf(inputPdf);
                    
                    // OPTIMIZATION: Only re-render failed pages, not entire PDF
                    result = retryFailedPagesOnly(repairedPdf, outputDir, dpi, format, failedPageNumbers, result);
                    failedCount = (Integer) result.get("failedPages");
                    
                    if (failedCount == 0) {
                        System.out.println("✓ QPDF repair successful - all pages recovered!");
                        result.put("repairMethod", "qpdf");
                    } else {
                        // Update failed page numbers for next attempt
                        @SuppressWarnings("unchecked")
                        List<String> remainingErrors = (List<String>) result.get("errors");
                        failedPageNumbers = extractPageNumbers(remainingErrors);
                    }
                } catch (IOException e) {
                    System.err.println("QPDF repair failed: " + e.getMessage());
                }
            }
            
            // Priority 3: If still failing, try Ghostscript (comprehensive, preserves quality)
            if (failedCount > 0 && repairService.isGhostscriptAvailable()) {
                try {
                    System.out.println("→ Strategy 3: Ghostscript repair...");
                    repairedPdf = repairService.repairWithGhostscript(inputPdf);
                    
                    // OPTIMIZATION: Only re-render still-failing pages
                    result = retryFailedPagesOnly(repairedPdf, outputDir, dpi, format, failedPageNumbers, result);
                    failedCount = (Integer) result.get("failedPages");
                    
                    if (failedCount == 0) {
                        System.out.println("✓ Ghostscript repair successful - all pages recovered!");
                        result.put("repairMethod", "ghostscript");
                    } else {
                        // Update failed page numbers for fallback
                        @SuppressWarnings("unchecked")
                        List<String> remainingErrors = (List<String>) result.get("errors");
                        failedPageNumbers = extractPageNumbers(remainingErrors);
                    }
                } catch (IOException e) {
                    System.err.println("Ghostscript repair failed: " + e.getMessage());
                }
            }
            
            // Priority 4: Last resort - try lower DPI (quality compromise)
            if (failedCount > 0 && dpi > 72) {
                System.out.println("→ Strategy 4 (Last Resort): Fallback to 72 DPI for remaining " + failedCount + " pages...");
                File pdfToUse = repairedPdf != null ? repairedPdf : inputPdf;
                
                // OPTIMIZATION: Only retry still-failing pages at 72 DPI
                result = retryFailedPagesAt72DPI(pdfToUse, outputDir, format, failedPageNumbers, result);
                failedCount = (Integer) result.get("failedPages");
                
                if (failedCount < failedPageNumbers.size()) {
                    result.put("repairMethod", result.containsKey("repairMethod") ? 
                        result.get("repairMethod") + "+dpi-fallback" : "dpi-fallback");
                }
            }
        }
            
            // Add overall timing
            long totalTime = System.currentTimeMillis() - overallStartTime;
            result.put("totalTimeSeconds", totalTime / 1000.0);
            
            return result;
            
        } finally {
            // OPTIMIZATION: Close document only once after all attempts
            try {
                document.close();
            } catch (IOException e) {
                System.err.println("Warning: Failed to close PDF document: " + e.getMessage());
            }
        }
    }
    
    /**
     * Extract page numbers from error messages.
     */
    private List<Integer> extractPageNumbers(List<String> errors) {
        List<Integer> pageNumbers = new ArrayList<>();
        if (errors == null) return pageNumbers;
        
        for (String error : errors) {
            try {
                // Extract "Page X:" from error message
                int pageStart = error.indexOf("Page ") + 5;
                int colonIndex = error.indexOf(':', pageStart);
                if (colonIndex > pageStart) {
                    String pageStr = error.substring(pageStart, colonIndex).trim();
                    pageNumbers.add(Integer.parseInt(pageStr));
                }
            } catch (Exception e) {
                // Skip malformed error messages
            }
        }
        return pageNumbers;
    }
    
    /**
     * Attempt conversion with provided PDDocument (OPTIMIZED - no reload).
     */
    private Map<String, Object> attemptConversionWithDocument(PDDocument document, File outputDir, int dpi, String format, String pdfFileName) throws IOException {
        long startTime = System.currentTimeMillis();
            
            int totalPages = document.getNumberOfPages();

            // Create output directory
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDir);
            }

            // Create PDF renderer
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Track file sizes
            List<MetadataGenerator.FileInfo> fileSizes = metadataGenerator.createFileInfoList();
            
            // Track failed pages
            List<String> failedPages = new ArrayList<>();

            // OPTIMIZATION: Adaptive thread count with configurable limits
            int numThreads = Math.min(
                Math.max(totalPages / pagesPerThread, minThreads),
                Math.min(maxThreads, Runtime.getRuntime().availableProcessors())
            );
            
            System.out.println("Processing " + totalPages + " pages with " + numThreads + " threads (ForkJoinPool)");
            
            // OPTIMIZATION: Use ForkJoinPool for better work-stealing
            ForkJoinPool executor = new ForkJoinPool(numThreads);
            AtomicInteger processedPages = new AtomicInteger(0);
            AtomicInteger successfulPages = new AtomicInteger(0);

            // Submit conversion tasks to ForkJoinPool
            try {
                executor.submit(() -> {
                    for (int i = 0; i < totalPages; i++) {
                        final int pageIndex = i;
                        final int pageNumber = i + 1;
                        try {
                            // Try with requested DPI only (no premature fallback)
                            BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
                            String filename = imageWriter.generateFilename(pageNumber, format);
                            File outputFile = new File(outputDir, filename);
                            long fileSize = imageWriter.writeImage(image, outputFile, format);

                            synchronized (fileSizes) {
                                fileSizes.add(metadataGenerator.createFileInfo(filename, fileSize, outputFile.getAbsolutePath()));
                            }

                            successfulPages.incrementAndGet();
                        } catch (Exception e) {
                            // Page failed - will be handled by repair service
                            String errorMsg = "Page " + pageNumber + ": " + e.getMessage();
                            System.err.println("Error processing " + errorMsg);
                            synchronized (failedPages) {
                                failedPages.add(errorMsg);
                            }
                        }
                        
                        processedPages.incrementAndGet();
                    }
                }).get(); // Wait for completion
            } catch (Exception e) {
                throw new IOException("Conversion task failed", e);
            } finally {
                // Shutdown ForkJoinPool
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        executor.shutdownNow();
                        throw new IOException("Conversion timed out");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                    throw new IOException("Conversion interrupted", e);
                }
            }

            // Calculate time
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;

            // Generate metadata file
            metadataGenerator.generateMetadata(
                    outputDir,
                    totalPages,
                    successfulPages.get(),
                    timeTaken,
                    dpi,
                    format,
                    pdfFileName,
                    fileSizes,
                    failedPages
            );

            // Return metadata as map
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalPages", totalPages);
            metadata.put("successfulPages", successfulPages.get());
            metadata.put("failedPages", failedPages.size());
            metadata.put("timeTakenSeconds", timeTaken / 1000.0);
            metadata.put("dpi", dpi);
            metadata.put("format", format);
            metadata.put("files", fileSizes);
            
            if (!failedPages.isEmpty()) {
                metadata.put("errors", failedPages);
                System.out.println("Warning: " + failedPages.size() + " page(s) failed to convert");
            }

            return metadata;
        // NOTE: Document is NOT closed here - handled by caller for reuse
    }
    
    
    /**
     * Retry only specific failed pages after repair (OPTIMIZATION).
     * This avoids re-rendering successful pages.
     */
    private Map<String, Object> retryFailedPagesOnly(File pdfFile, File outputDir, int dpi, 
                                                      String format, List<Integer> failedPageNumbers,
                                                      Map<String, Object> previousResult) throws IOException {
        if (failedPageNumbers.isEmpty()) {
            return previousResult;
        }

        System.out.println("  Retrying " + failedPageNumbers.size() + " failed page(s) only...");
        
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile, org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly());
            document.setAllSecurityToBeRemoved(true);

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            @SuppressWarnings("unchecked")
            List<MetadataGenerator.FileInfo> existingFiles = (List<MetadataGenerator.FileInfo>) previousResult.get("files");
            List<String> newErrors = new ArrayList<>();
            int recovered = 0;
            
            // Only process pages that failed before
            for (int pageNumber : failedPageNumbers) {
                int pageIndex = pageNumber - 1;
                
                try {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
                    String filename = imageWriter.generateFilename(pageNumber, format);
                    File outputFile = new File(outputDir, filename);
                    long fileSize = imageWriter.writeImage(image, outputFile, format);
                    
                    existingFiles.add(metadataGenerator.createFileInfo(filename, fileSize, outputFile.getAbsolutePath()));
                    recovered++;
                } catch (Exception e) {
                    newErrors.add("Page " + pageNumber + ": " + e.getMessage());
                }
            }
            
            // Update result
            int successfulPages = (Integer) previousResult.get("successfulPages") + recovered;
            previousResult.put("successfulPages", successfulPages);
            previousResult.put("failedPages", newErrors.size());
            previousResult.put("files", existingFiles);
            
            if (!newErrors.isEmpty()) {
                previousResult.put("errors", newErrors);
            } else {
                previousResult.remove("errors");
            }
            
            if (recovered > 0) {
                System.out.println("  ✓ Recovered " + recovered + " of " + failedPageNumbers.size() + " pages");
            }
            
            return previousResult;
            
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close PDF document: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Retry failed pages at 72 DPI (OPTIMIZATION).
     * Only processes pages that are still failing.
     */
    private Map<String, Object> retryFailedPagesAt72DPI(File pdfFile, File outputDir, String format,
                                                         List<Integer> failedPageNumbers,
                                                         Map<String, Object> previousResult) throws IOException {
        if (failedPageNumbers.isEmpty()) {
            return previousResult;
        }

        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile, org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly());
            document.setAllSecurityToBeRemoved(true);

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            @SuppressWarnings("unchecked")
            List<MetadataGenerator.FileInfo> existingFiles = (List<MetadataGenerator.FileInfo>) previousResult.get("files");
            List<String> stillFailing = new ArrayList<>();
            int recovered = 0;
            
            // Only retry pages that are still failing
            for (int pageNumber : failedPageNumbers) {
                int pageIndex = pageNumber - 1;
                
                try {
                    // Try rendering at 72 DPI
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 72);
                    String filename = imageWriter.generateFilename(pageNumber, format);
                    File outputFile = new File(outputDir, filename);
                    long fileSize = imageWriter.writeImage(image, outputFile, format);
                    
                    existingFiles.add(metadataGenerator.createFileInfo(filename, fileSize, outputFile.getAbsolutePath()));
                    recovered++;
                    
                    System.out.println("✓ Page " + pageNumber + " recovered at 72 DPI");
                } catch (Exception e) {
                    stillFailing.add("Page " + pageNumber + " (72 DPI also failed): " + e.getMessage());
                }
            }
            
            // Update result
            int successfulPages = (Integer) previousResult.get("successfulPages") + recovered;
            previousResult.put("successfulPages", successfulPages);
            previousResult.put("failedPages", stillFailing.size());
            previousResult.put("files", existingFiles);
            
            if (!stillFailing.isEmpty()) {
                previousResult.put("errors", stillFailing);
            } else {
                previousResult.remove("errors");
            }
            
            if (recovered > 0) {
                System.out.println("✓ Recovered " + recovered + " page(s) using 72 DPI fallback");
            }
            
            return previousResult;
            
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close PDF document: " + e.getMessage());
                }
            }
        }
    }
}
