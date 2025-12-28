package com.pdfconverter.core;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Service for quick PDF validation before attempting conversion.
 * Performs fast checks to detect corrupted or invalid PDFs.
 */
@Service
public class PdfValidationService {

    /**
     * Validation result containing PDF health status.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final boolean isEncrypted;
        private final boolean hasStructureIssues;
        private final int pageCount;
        private final String error;

        public ValidationResult(boolean isValid, boolean isEncrypted, boolean hasStructureIssues, 
                               int pageCount, String error) {
            this.isValid = isValid;
            this.isEncrypted = isEncrypted;
            this.hasStructureIssues = hasStructureIssues;
            this.pageCount = pageCount;
            this.error = error;
        }

        public boolean isValid() { return isValid; }
        public boolean isEncrypted() { return isEncrypted; }
        public boolean hasStructureIssues() { return hasStructureIssues; }
        public int getPageCount() { return pageCount; }
        public String getError() { return error; }
        
        public boolean needsRepair() {
            return isValid && hasStructureIssues;
        }
    }

    /**
     * Quickly validate PDF file.
     * @param pdfFile PDF file to validate
     * @return Validation result
     */
    public ValidationResult validatePdf(File pdfFile) {
        // Quick file checks
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            return new ValidationResult(false, false, false, 0, "File does not exist");
        }

        if (pdfFile.length() == 0) {
            return new ValidationResult(false, false, false, 0, "File is empty");
        }

        // Check PDF header
        if (!hasValidPdfHeader(pdfFile)) {
            return new ValidationResult(false, false, false, 0, "Invalid PDF header");
        }

        // Try loading with PDFBox
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile, 
                org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly());
            
            boolean isEncrypted = document.isEncrypted();
            int pageCount = document.getNumberOfPages();
            
            if (pageCount == 0) {
                return new ValidationResult(false, isEncrypted, false, 0, "PDF has no pages");
            }

            // PDF is valid - structure issues will be detected during conversion
            return new ValidationResult(true, isEncrypted, false, pageCount, null);
            
        } catch (IOException e) {
            // PDF exists but has structure issues - may need repair
            String error = e.getMessage();
            boolean likelyStructureIssue = error.contains("Expected") || 
                                           error.contains("Invalid") ||
                                           error.contains("damaged");
            
            return new ValidationResult(true, false, likelyStructureIssue, 0, error);
            
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Check if file has valid PDF header (%PDF-).
     */
    private boolean hasValidPdfHeader(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 5) {
                return false;
            }
            
            byte[] header = new byte[5];
            raf.readFully(header);
            
            String headerStr = new String(header);
            return headerStr.equals("%PDF-");
            
        } catch (IOException e) {
            return false;
        }
    }
}
