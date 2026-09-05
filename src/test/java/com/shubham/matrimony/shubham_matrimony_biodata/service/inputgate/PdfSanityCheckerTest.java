package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfSanityCheckerTest {

    private PdfSanityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PdfSanityChecker();
    }

    @Test
    void testDigitalPdfTextExtractionSuccessful() throws IOException {
        byte[] pdfBytes = createPdfWithText(1,
                "Name: Satwik Kotte\n" +
                        "Date of Birth: 15-08-1995\n" +
                        "Height: 5ft 10in\n" +
                        "Education: Bachelor of Engineering in Computer Science\n" +
                        "Occupation: Software Engineer\n" +
                        "Native Place: Hyderabad\n");

        PdfSanityChecker.PdfAnalysisResult result = checker.inspectAndExtract(pdfBytes);

        assertFalse(result.isScanned(), "Expected digital text PDF, not scanned");
        assertEquals(1, result.getPageCount());
        assertNotNull(result.getExtractedText());
        assertTrue(result.getExtractedText().contains("Satwik Kotte"));
        assertTrue(result.getExtractedText().contains("Software Engineer"));
    }

    @Test
    void testExcessivePageCountRejected() throws IOException {
        byte[] largePdfBytes = createBlankPdf(12);

        InputGateException ex = assertThrows(InputGateException.class, () -> checker.inspectAndExtract(largePdfBytes));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(WarningCategory.EXCESSIVE_PAGE_COUNT, ex.getCategory());
        assertTrue(ex.getMessage().contains("exceeds maximum allowed length of 10 pages"));
    }

    @Test
    void testCorruptedPdfPayloadRejected() {
        byte[] corrupted = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x00, 0x11, 0x22 }; // broken %PDF stream

        InputGateException ex = assertThrows(InputGateException.class, () -> checker.inspectAndExtract(corrupted));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(WarningCategory.CORRUPTED_DOCUMENT, ex.getCategory());
    }

    @Test
    void testScannedPdfWithoutTextMarkedAsScanned() throws IOException {
        byte[] blankPagePdf = createBlankPdf(1);

        PdfSanityChecker.PdfAnalysisResult result = checker.inspectAndExtract(blankPagePdf);

        assertTrue(result.isScanned(), "Page without digital text should be marked as scanned");
        assertEquals(1, result.getPageCount());
        assertNull(result.getExtractedText());
    }

    private byte[] createPdfWithText(int pageCount, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (i == 0 && text != null) {
                    try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 700);
                        for (String line : text.split("\n")) {
                            stream.showText(line);
                            stream.newLineAtOffset(0, -18);
                        }
                        stream.endText();
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createBlankPdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
