package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Validates PDF structure and page count limits, and extracts digital text locally at zero cost.
 */
@Slf4j
@Component
public class PdfSanityChecker {

    private static final int MAX_ALLOWED_PAGES = 10;
    private static final int MIN_DIGITAL_TEXT_CHARS = 50;

    @Value
    public static class PdfAnalysisResult {
        boolean scanned;
        int pageCount;
        String extractedText;
    }

    /**
     * Inspects PDF bytes, enforces page limits, and attempts local digital text extraction.
     *
     * @param pdfBytes raw PDF payload
     * @return analysis result containing page count and extracted text (if digital)
     */
    public PdfAnalysisResult inspectAndExtract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            int pages = document.getNumberOfPages();

            if (pages == 0) {
                throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.CORRUPTED_DOCUMENT,
                        "PDF contains 0 pages.");
            }

            if (pages > MAX_ALLOWED_PAGES) {
                throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.EXCESSIVE_PAGE_COUNT,
                        "PDF exceeds maximum allowed length of " + MAX_ALLOWED_PAGES
                                + " pages for a matrimonial biodata. Found " + pages + " pages.");
            }

            // Attempt zero-cost local digital text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text != null && text.trim().length() >= MIN_DIGITAL_TEXT_CHARS) {
                log.info("Extracted {} characters of digital text locally from {}-page PDF ($0 OCR cost)",
                        text.trim().length(), pages);
                return new PdfAnalysisResult(false, pages, text.trim());
            }

            log.info("PDF has {} pages with insufficient digital text (< {} chars). Marked as scanned document.",
                    pages, MIN_DIGITAL_TEXT_CHARS);
            return new PdfAnalysisResult(true, pages, null);

        } catch (InputGateException ige) {
            throw ige;
        } catch (IOException e) {
            log.warn("Corrupted or invalid PDF payload: {}", e.getMessage());
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.CORRUPTED_DOCUMENT,
                    "PDF structure is invalid or file is corrupted.", e);
        } catch (Exception e) {
            log.error("Unexpected error reading PDF: {}", e.getMessage(), e);
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.CORRUPTED_DOCUMENT,
                    "Unable to parse PDF document.", e);
        }
    }
}
