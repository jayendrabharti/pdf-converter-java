package com.pdfconverter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating ZIP archives.
 */
public class ZipUtility {

    /**
     * Zips an entire directory into a ZIP file.
     *
     * @param sourceDir Source directory to zip
     * @param zipFile   Output ZIP file
     * @throws IOException if zipping fails
     */
    public static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zipDirectoryRecursive(sourceDir, sourceDir, zos);
        }
    }

    /**
     * Recursively adds files to ZIP archive.
     *
     * @param rootDir   Root directory
     * @param sourceDir Current directory
     * @param zos       ZipOutputStream
     * @throws IOException if adding files fails
     */
    private static void zipDirectoryRecursive(File rootDir, File sourceDir, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryRecursive(rootDir, file, zos);
            } else {
                addFileToZip(rootDir, file, zos);
            }
        }
    }

    /**
     * Adds a single file to the ZIP archive.
     *
     * @param rootDir Root directory (for relative path calculation)
     * @param file    File to add
     * @param zos     ZipOutputStream
     * @throws IOException if adding file fails
     */
    private static void addFileToZip(File rootDir, File file, ZipOutputStream zos) throws IOException {
        String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
        ZipEntry zipEntry = new ZipEntry(relativePath);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
    }
}
