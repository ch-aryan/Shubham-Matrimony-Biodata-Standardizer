package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MagicByteValidatorTest {

    private MagicByteValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MagicByteValidator();
    }

    @Test
    void testValidPdfAccepted() {
        byte[] pdfBytes = "%PDF-1.4\nSome pdf content".getBytes(StandardCharsets.US_ASCII);
        MagicByteValidator.DocumentType type = validator.validate("biodata.pdf", pdfBytes);
        assertEquals(MagicByteValidator.DocumentType.PDF, type);
    }

    @Test
    void testValidJpegAccepted() {
        byte[] jpegBytes = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I',
                'F' };
        MagicByteValidator.DocumentType type = validator.validate("photo.jpg", jpegBytes);
        assertEquals(MagicByteValidator.DocumentType.JPEG, type);
    }

    @Test
    void testValidPngAccepted() {
        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00 };
        MagicByteValidator.DocumentType type = validator.validate("biodata.png", pngBytes);
        assertEquals(MagicByteValidator.DocumentType.PNG, type);
    }

    @Test
    void testValidWebpAccepted() {
        byte[] webpBytes = new byte[] {
                'R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
        MagicByteValidator.DocumentType type = validator.validate("document.webp", webpBytes);
        assertEquals(MagicByteValidator.DocumentType.WEBP, type);
    }

    @Test
    void testValidPlainTextAccepted() {
        byte[] textBytes = "Name: Satwik\nDOB: 12-05-1995\nHeight: 5ft 8in".getBytes(StandardCharsets.UTF_8);
        MagicByteValidator.DocumentType type = validator.validate("biodata.txt", textBytes);
        assertEquals(MagicByteValidator.DocumentType.PLAIN_TEXT, type);
    }

    @Test
    void testExecutableDisguisedAsPdfRejected() {
        byte[] mzBytes = new byte[] { 'M', 'Z', 0x00, 0x00, 0x01, 0x00 };
        InputGateException ex = assertThrows(InputGateException.class,
                () -> validator.validate("biodata.pdf", mzBytes));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(WarningCategory.INVALID_FILE_SIGNATURE, ex.getCategory());
        assertTrue(ex.getMessage().contains("Rejected executable file"));
    }

    @Test
    void testProhibitedExtensionDocxRejected() {
        byte[] pkBytes = new byte[] { 'P', 'K', 0x03, 0x04, 0x14, 0x00 };
        InputGateException ex = assertThrows(InputGateException.class,
                () -> validator.validate("biodata.docx", pkBytes));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getStatus());
        assertEquals(WarningCategory.UNSUPPORTED_MEDIA_TYPE, ex.getCategory());
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    void testPdfMismatchRejected() {
        byte[] fakePdfBytes = "Random ASCII text without PDF header".getBytes(StandardCharsets.UTF_8);
        InputGateException ex = assertThrows(InputGateException.class,
                () -> validator.validate("fake.pdf", fakePdfBytes));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(WarningCategory.INVALID_FILE_SIGNATURE, ex.getCategory());
        assertTrue(ex.getMessage().contains("header signature does not match '%PDF'"));
    }

    @Test
    void testEmptyFileRejected() {
        InputGateException ex = assertThrows(InputGateException.class,
                () -> validator.validate("empty.pdf", new byte[0]));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(WarningCategory.LOW_INFORMATION_INPUT, ex.getCategory());
    }
}
