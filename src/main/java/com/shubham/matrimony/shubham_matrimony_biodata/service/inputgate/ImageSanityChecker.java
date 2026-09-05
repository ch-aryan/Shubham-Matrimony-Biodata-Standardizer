package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Performs fast, local sanity checks on image streams (dimensions, contrast,
 * blank/monochromatic detection)
 * before any OCR or external AI calls are dispatched.
 */
@Slf4j
@Component
public class ImageSanityChecker {

    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 200;
    private static final double MIN_CONTRAST_STDDEV = 8.0;

    /**
     * Inspects image bytes to ensure valid decoding, minimum readable dimensions,
     * and non-blank/non-black content.
     *
     * @param imageBytes raw image bytes
     */
    public void validateImageSanity(byte[] imageBytes) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            log.warn("Failed to decode image bytes: {}", e.getMessage());
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.CORRUPTED_DOCUMENT,
                    "Image stream cannot be decoded or is corrupted.", e);
        }

        if (image == null) {
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.CORRUPTED_DOCUMENT,
                    "Image stream cannot be decoded or is corrupted.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.BLANK_OR_UNREADABLE_IMAGE,
                    "Image resolution is too low (" + width + "x" + height + "). Minimum "
                            + MIN_WIDTH + "x" + MIN_HEIGHT + " required for readable biodata.");
        }

        // Fast pixel variance check across a sampled grid
        checkImageContrastAndContent(image);
    }

    private void checkImageContrastAndContent(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int stepX = Math.max(1, width / 100);
        int stepY = Math.max(1, height / 100);

        long count = 0;
        double sumLum = 0.0;
        double sumSqLum = 0.0;

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Standard ITU-R BT.601 perceptual luminance
                double lum = 0.299 * r + 0.587 * g + 0.114 * b;
                sumLum += lum;
                sumSqLum += (lum * lum);
                count++;
            }
        }

        if (count == 0) {
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.BLANK_OR_UNREADABLE_IMAGE,
                    "Image contains no sampleable pixels.");
        }

        double mean = sumLum / count;
        double variance = (sumSqLum / count) - (mean * mean);
        double stddev = Math.sqrt(Math.max(0.0, variance));

        log.debug("Image sanity stats: sampled {} pixels, mean luminance = {}, stddev = {}", count, mean, stddev);

        // Solid color or virtually uniform canvas
        if (stddev < MIN_CONTRAST_STDDEV) {
            String appearance = mean > 240 ? "solid white / blank" : (mean < 15 ? "solid black" : "monochromatic");
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.BLANK_OR_UNREADABLE_IMAGE,
                    "Rejected " + appearance + " image with insufficient contrast (stddev: "
                            + String.format("%.2f", stddev) + ") to contain readable biodata.");
        }

        // Near-pure white or near-pure black screen with barely noticeable noise
        if ((mean > 252.0 && stddev < 12.0) || (mean < 5.0 && stddev < 12.0)) {
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.BLANK_OR_UNREADABLE_IMAGE,
                    "Rejected blank image with minimal pixel variation.");
        }
    }
}
