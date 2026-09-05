package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageSanityCheckerTest {

    private ImageSanityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ImageSanityChecker();
    }

    @Test
    void testSolidWhiteImageRejected() throws IOException {
        byte[] whiteImage = createTestImage(400, 400, Color.WHITE, null);
        InputGateException ex = assertThrows(InputGateException.class, () ->
                checker.validateImageSanity(whiteImage));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(WarningCategory.BLANK_OR_UNREADABLE_IMAGE, ex.getCategory());
        assertTrue(ex.getMessage().contains("solid white / blank") || ex.getMessage().contains("blank image"));
    }

    @Test
    void testSolidBlackImageRejected() throws IOException {
        byte[] blackImage = createTestImage(400, 400, Color.BLACK, null);
        InputGateException ex = assertThrows(InputGateException.class, () ->
                checker.validateImageSanity(blackImage));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(WarningCategory.BLANK_OR_UNREADABLE_IMAGE, ex.getCategory());
        assertTrue(ex.getMessage().contains("solid black") || ex.getMessage().contains("blank image"));
    }

    @Test
    void testTinyImageResolutionRejected() throws IOException {
        byte[] tinyImage = createTestImage(100, 100, Color.WHITE, "Tiny");
        InputGateException ex = assertThrows(InputGateException.class, () ->
                checker.validateImageSanity(tinyImage));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(WarningCategory.BLANK_OR_UNREADABLE_IMAGE, ex.getCategory());
        assertTrue(ex.getMessage().contains("resolution is too low"));
    }

    @Test
    void testValidHighContrastImageAccepted() throws IOException {
        byte[] biodataImage = createTestImage(600, 800, Color.WHITE, "BIODATA\nName: Rohan Sharma\nDOB: 12-04-1994");
        assertDoesNotThrow(() -> checker.validateImageSanity(biodataImage));
    }

    @Test
    void testCorruptedImageStreamRejected() {
        byte[] corruptBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        InputGateException ex = assertThrows(InputGateException.class, () ->
                checker.validateImageSanity(corruptBytes));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(WarningCategory.CORRUPTED_DOCUMENT, ex.getCategory());
    }

    private byte[] createTestImage(int width, int height, Color bgColor, String text) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);

        if (text != null && !text.isBlank()) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            int y = 50;
            for (String line : text.split("\n")) {
                g.drawString(line, 40, y);
                y += 40;
            }
            // Draw a few dark lines to simulate printed document content
            g.fillRect(40, 180, 400, 10);
            g.fillRect(40, 220, 300, 10);
            g.fillRect(40, 260, 350, 10);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
