package com.pdfconverter.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Service for repairing problematic PDF files using external tools.
 * Supports QPDF for fast repairs and Ghostscript for comprehensive fixes.
 */
@Service
public class PdfRepairService {

    @Value("${app.repair.enabled:true}")
    private boolean repairEnabled;

    @Value("${app.repair.qpdf.path:qpdf}")
    private String qpdfPath;

    @Value("${app.repair.ghostscript.path:gs}")
    private String ghostscriptPath;

    @Value("${app.repair.timeout-seconds:300}")
    private int timeoutSeconds;

    private boolean qpdfAvailable = false;
    private boolean ghostscriptAvailable = false;

    /**
     * Check availability of repair tools on startup.
     */
    @PostConstruct
    public void checkToolAvailability() {
        if (!repairEnabled) {
            System.out.println("PDF repair is disabled");
            return;
        }

        qpdfAvailable = checkCommand(qpdfPath, "--version");
        ghostscriptAvailable = checkCommand(ghostscriptPath, "--version");

        System.out.println("PDF Repair Tools:");
        System.out.println("  QPDF: " + (qpdfAvailable ? "✓ Available" : "✗ Not found"));
        System.out.println("  Ghostscript: " + (ghostscriptAvailable ? "✓ Available" : "✗ Not found"));

        if (!qpdfAvailable && !ghostscriptAvailable) {
            System.out.println("⚠ Warning: No repair tools available. Install QPDF or Ghostscript for better PDF compatibility.");
        }
    }

    /**
     * Check if a command is available on the system.
     */
    private boolean checkCommand(String command, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (args.length > 0) {
                pb.command().add(args[0]);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0 || process.exitValue() == 1; // Some tools return 1 for --version
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Repair PDF using QPDF (fast linearization).
     */
    public File repairWithQpdf(File inputPdf) throws IOException {
        if (!qpdfAvailable) {
            throw new IOException("QPDF is not available");
        }

        File outputPdf = createTempFile("qpdf-repaired", ".pdf");

        ProcessBuilder pb = new ProcessBuilder(
            qpdfPath,
            "--linearize",
            inputPdf.getAbsolutePath(),
            outputPdf.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        
        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Capture output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("QPDF repair timed out after " + timeoutSeconds + " seconds");
            }

            if (process.exitValue() != 0) {
                throw new IOException("QPDF repair failed: " + output.toString());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("QPDF repair completed in " + (elapsed / 1000.0) + "s");

            return outputPdf;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("QPDF repair was interrupted", e);
        }
    }

    /**
     * Repair PDF using Ghostscript (comprehensive repair with quality preservation).
     */
    public File repairWithGhostscript(File inputPdf) throws IOException {
        if (!ghostscriptAvailable) {
            throw new IOException("Ghostscript is not available");
        }

        File outputPdf = createTempFile("gs-repaired", ".pdf");

        ProcessBuilder pb = new ProcessBuilder(
            ghostscriptPath,
            "-sDEVICE=pdfwrite",
            "-dNOPAUSE",
            "-dBATCH",
            "-dSAFER",
            "-dPDFSETTINGS=/prepress",  // Maximum quality - no downsampling
            "-dColorConversionStrategy=/LeaveColorUnchanged",  // Preserve colors
            "-dDownsampleMonoImages=false",  // No downsampling
            "-dDownsampleGrayImages=false",
            "-dDownsampleColorImages=false",
            "-dAutoFilterColorImages=false",
            "-dAutoFilterGrayImages=false",
            "-sOutputFile=" + outputPdf.getAbsolutePath(),
            inputPdf.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        
        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Capture output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Ghostscript repair timed out after " + timeoutSeconds + " seconds");
            }

            if (process.exitValue() != 0) {
                throw new IOException("Ghostscript repair failed: " + output.toString());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Ghostscript repair completed in " + (elapsed / 1000.0) + "s");

            return outputPdf;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ghostscript repair was interrupted", e);
        }
    }

    /**
     * Create a temporary file for repair output.
     */
    private File createTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }

    // Getters for availability checks
    public boolean isRepairEnabled() {
        return repairEnabled;
    }

    public boolean isQpdfAvailable() {
        return qpdfAvailable;
    }

    public boolean isGhostscriptAvailable() {
        return ghostscriptAvailable;
    }

    public boolean isAnyRepairAvailable() {
        return repairEnabled && (qpdfAvailable || ghostscriptAvailable);
    }
}
