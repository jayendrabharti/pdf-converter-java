package com.pdfconverter.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates metadata.json file with conversion statistics.
 */
public class MetadataGenerator {

    public static class Metadata {
        private final int totalPages;
        private final int successfulPages;
        private final int failedPages;
        private final double timeTakenSeconds;
        private final int dpi;
        private final String outputFormat;
        private final List<FileInfo> files;
        private final List<String> errors;
        private final String timestamp;
        private final String inputFile;

        public Metadata(int totalPages, int successfulPages, double timeTakenSeconds, int dpi,
                       String outputFormat, List<FileInfo> files, String inputFile, List<String> errors) {
            this.totalPages = totalPages;
            this.successfulPages = successfulPages;
            this.failedPages = totalPages - successfulPages;
            this.timeTakenSeconds = timeTakenSeconds;
            this.dpi = dpi;
            this.outputFormat = outputFormat;
            this.files = files;
            this.errors = errors.isEmpty() ? null : errors;
            this.timestamp = Instant.now().toString();
            this.inputFile = inputFile;
        }
    }

    public static class FileInfo {
        private final String page;
        private final long sizeBytes;
        private final String path;

        public FileInfo(String page, long sizeBytes, String path) {
            this.page = page;
            this.sizeBytes = sizeBytes;
            this.path = path;
        }

        public String getPage() {
            return page;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public String getPath() {
            return path;
        }
    }

    public void generateMetadata(File outputDir, int totalPages, int successfulPages, long timeTakenMs,
                                 int dpi, String format, String inputFileName,
                                 List<FileInfo> files, List<String> errors) throws IOException {
        double timeTakenSeconds = timeTakenMs / 1000.0;

        Metadata metadata = new Metadata(
                totalPages,
                successfulPages,
                timeTakenSeconds,
                dpi,
                format,
                files,
                inputFileName,
                errors
        );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(metadata);

        File metadataFile = new File(outputDir, "metadata.json");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            writer.write(json);
        }
    }

    public FileInfo createFileInfo(String filename, long sizeBytes, String absolutePath) {
        return new FileInfo(filename, sizeBytes, absolutePath);
    }

    public List<FileInfo> createFileInfoList() {
        return new ArrayList<>();
    }
}
