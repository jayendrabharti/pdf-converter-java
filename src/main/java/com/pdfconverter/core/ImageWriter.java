package com.pdfconverter.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Handles writing BufferedImage objects to disk with optimized compression settings.
 * Supports configurable JPEG quality and PNG compression levels.
 */
@Component
public class ImageWriter {

    @Value("${app.image.jpeg-quality:0.90}")
    private float jpegQuality;

    @Value("${app.image.png-compression-level:6}")
    private int pngCompressionLevel;

    public long writeImage(BufferedImage image, File outputFile, String format) throws IOException {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + parentDir);
        }

        BufferedImage imageToWrite = image;
        
        // Convert to RGB for JPEG (JPEG doesn't support alpha channel)
        if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
            imageToWrite = convertToRGB(image);
            writeJPEGWithQuality(imageToWrite, outputFile, jpegQuality);
        } else if (format.equalsIgnoreCase("png")) {
            writePNGWithCompression(imageToWrite, outputFile, pngCompressionLevel);
        } else {
            // Fallback to default ImageIO
            boolean success = ImageIO.write(imageToWrite, format, outputFile);
            if (!success) {
                throw new IOException("Failed to write image in format: " + format);
            }
        }

        return outputFile.length();
    }

    /**
     * Write JPEG with specified quality setting.
     * @param image Image to write
     * @param outputFile Output file
     * @param quality Quality from 0.0 to 1.0 (0.9 = 90% quality)
     */
    private void writeJPEGWithQuality(BufferedImage image, File outputFile, float quality) throws IOException {
        Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }

        javax.imageio.ImageWriter writer = writers.next();
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, quality)));

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), jpegParams);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Write PNG with specified compression level.
     * @param image Image to write
     * @param outputFile Output file
     * @param compressionLevel Compression from 0-9 (6 is balanced)
     */
    private void writePNGWithCompression(BufferedImage image, File outputFile, int compressionLevel) throws IOException {
        Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IOException("No PNG writer found");
        }

        javax.imageio.ImageWriter writer = writers.next();
        ImageWriteParam pngParams = writer.getDefaultWriteParam();
        
        if (pngParams.canWriteCompressed()) {
            pngParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // PNG compression is 0-9, normalize to 0.0-1.0 for ImageIO
            float normalizedCompression = compressionLevel / 9.0f;
            pngParams.setCompressionQuality(1.0f - normalizedCompression); // ImageIO uses inverse scale
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), pngParams);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Convert image to RGB format (required for JPEG).
     */
    private BufferedImage convertToRGB(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage rgbImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        rgbImage.createGraphics().drawImage(source, 0, 0, null);
        return rgbImage;
    }

    public String generateFilename(int pageNumber, String format) {
        return String.format("page-%03d.%s", pageNumber, format);
    }
}
