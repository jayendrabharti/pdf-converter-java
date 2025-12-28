package com.pdfconverter.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Handles writing BufferedImage objects to disk as JPG or PNG files.
 */
public class ImageWriter {

    public long writeImage(BufferedImage image, File outputFile, String format) throws IOException {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + parentDir);
        }

        BufferedImage imageToWrite = image;
        if (format.equalsIgnoreCase("jpg")) {
            imageToWrite = convertToRGB(image);
        }

        boolean success = ImageIO.write(imageToWrite, format, outputFile);
        if (!success) {
            throw new IOException("Failed to write image in format: " + format);
        }

        return outputFile.length();
    }

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
