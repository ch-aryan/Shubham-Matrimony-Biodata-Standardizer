package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataServiceParser;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Gatekeeper service enforcing zero-cost local validation before dispatching to
 * deterministic parsing or Gemini Multimodal document understanding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InputGateService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final MagicByteValidator magicByteValidator;
    private final ImageSanityChecker imageSanityChecker;
    private final PdfSanityChecker pdfSanityChecker;
    private final BiodataServiceParser biodataService;
    private final GeminiService geminiService;

    /**
     * Ingests, validates, extracts raw text, and invokes the parsing pipeline.
     *
     * @param file uploaded document/image file
     * @param forceAi whether to force AI downstream semantic review
     * @return populated {@link ParseResponse}
     */
    public ParseResponse processUpload(MultipartFile file, Boolean forceAi) {
        if (file == null || file.isEmpty()) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.LOW_INFORMATION_INPUT,
                    "Uploaded file is empty or missing.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InputGateException(HttpStatus.PAYLOAD_TOO_LARGE, WarningCategory.LOW_INFORMATION_INPUT,
                    "File size (" + (file.getSize() / (1024 * 1024)) + "MB) exceeds maximum allowed size of 10MB.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.CORRUPTED_DOCUMENT,
                    "Unable to read uploaded file stream.", e);
        }

        String originalFilename = file.getOriginalFilename();
        MagicByteValidator.DocumentType docType = magicByteValidator.validate(originalFilename, bytes);

        String rawText;

        switch (docType) {
            case PLAIN_TEXT -> {
                rawText = new String(bytes, StandardCharsets.UTF_8).trim();
                if (rawText.isBlank()) {
                    throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.LOW_INFORMATION_INPUT,
                            "Uploaded text file is blank.");
                }
            }
            case PDF -> {
                PdfSanityChecker.PdfAnalysisResult analysis = pdfSanityChecker.inspectAndExtract(bytes);
                if (!analysis.isScanned()) {
                    // Local digital text obtained at $0 cost!
                    rawText = analysis.getExtractedText();
                } else {
                    // Scanned / raster PDF -> requires Multimodal Document Understanding
                    rawText = extractViaMultimodal(bytes, "application/pdf");
                }
            }
            case JPEG, PNG, WEBP -> {
                imageSanityChecker.validateImageSanity(bytes);
                rawText = extractViaMultimodal(bytes, docType.getMimeType());
            }
            default -> throw new InputGateException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, WarningCategory.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported document format: " + docType);
        }

        if (rawText == null || rawText.isBlank()) {
            return ParseResponse.builder()
                    .status(ParseStatus.REJECTED_INPUT)
                    .profile(null)
                    .warnings(List.of(ParseWarning.builder()
                            .category(WarningCategory.LOW_INFORMATION_INPUT)
                            .message("No readable text could be extracted from the document.")
                            .build()))
                    .build();
        }

        // Feed extracted raw text into the deterministic parser and semantic pipeline
        return biodataService.parseAndValidate(rawText, forceAi);
    }

    private String extractViaMultimodal(byte[] bytes, String mimeType) {
        if (geminiService == null || !geminiService.isAvailable()) {
            throw new InputGateException(HttpStatus.SERVICE_UNAVAILABLE, WarningCategory.AI_SERVICE_UNAVAILABLE,
                    "Document OCR / multimodal understanding requires Gemini API key configuration.");
        }

        log.info("Sending {}-byte document ({}) to Gemini Multimodal OCR...", bytes.length, mimeType);
        Optional<String> ocrResult = geminiService.extractDocumentText(bytes, mimeType);

        if (ocrResult.isEmpty() || ocrResult.get().isBlank()) {
            throw new InputGateException(HttpStatus.UNPROCESSABLE_ENTITY, WarningCategory.LOW_INFORMATION_INPUT,
                    "Gemini OCR could not extract any readable text from the supplied document.");
        }

        return ocrResult.get().trim();
    }
}
