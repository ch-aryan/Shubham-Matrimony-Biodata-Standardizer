package com.shubham.matrimony.shubham_matrimony_biodata.controller;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseRequest;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataServiceParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biodata")
public class BiodataController {

    /**
     * Maximum allowed input size in characters.
     * A normal WhatsApp biodata is 500–3000 chars. 50K is generous for
     * OCR-extracted PDFs.
     * If input exceeds this, we reject before parsing — never silently truncate.
     */
    private static final int MAX_INPUT_LENGTH = 50_000;

    private final BiodataServiceParser biodataService;
    private final com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate.InputGateService inputGateService;

    public BiodataController(BiodataServiceParser biodataService,
            com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate.InputGateService inputGateService) {
        this.biodataService = biodataService;
        this.inputGateService = inputGateService;
    }

    /**
     * Main production endpoint.
     *
     * <p>
     * Controller validates the HTTP envelope (null, blank, oversized).
     * Engine validates whether the text is suitable for parsing.
     *
     * @param request JSON body with {@code rawText} field
     * @return {@link ParseResponse} with status, profile, warnings, and unparsed
     *         lines
     */
    @PostMapping("/parse")
    public ResponseEntity<ParseResponse> parse(@RequestBody ParseRequest request) {

    public ResponseEntity<ParseResponse> parse(@jakarta.validation.Valid @RequestBody ParseRequest request) {

        // Check 1: is rawText present?
        if (request.getRawText() == null || request.getRawText().isBlank()) {
            return ResponseEntity.badRequest().body(rejectedResponse("Biodata text is required."));
        }

        // Check 2: is rawText within processable size?
        if (request.getRawText().length() > MAX_INPUT_LENGTH) {
            return ResponseEntity.badRequest().body(rejectedResponse(
                    "Input exceeds maximum supported size (" + MAX_INPUT_LENGTH
                            + " characters). Please provide a smaller biodata."));
        }

        // Delegate everything else to the engine
        return ResponseEntity.ok(biodataService.parseAndValidate(request.getRawText(), request.getForceAi()));
    }

    /**
     * Multimodal upload endpoint for PDF documents and image files (JPEG, PNG,
     * WEBP).
     *
     * <p>
     * Enforces zero-cost local sanity checks (blank/black image rejection, PDF
     * structure/page count,
     * magic byte validation) before processing.
     *
     * @param file    uploaded document or image
     * @param forceAi optional flag to force downstream Gemini semantic review
     * @return {@link ParseResponse} with status, profile, warnings, and unparsed
     *         lines
     */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParseResponse> upload(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "forceAi", required = false) Boolean forceAi) {
        return ResponseEntity.ok(inputGateService.processUpload(file, forceAi));
    }

    /**
     * Lightweight raw-text endpoint for quick dev/testing.
     * Returns only the populated {@link ProfileBiodata} without validation
     * wrapping.
     */
    @PostMapping("/parseraw")
    public ProfileBiodata parseRaw(@RequestBody String rawText) {
        return biodataService.parse(rawText);
    }

    @GetMapping("/health")
    public String check() {
        return "biodata service is running";
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ParseResponse rejectedResponse(String message) {
        return ParseResponse.builder()
                .status(ParseStatus.REJECTED_INPUT)
                .profile(null)
                .warnings(List.of(
                        ParseWarning.builder()
                                .category(WarningCategory.LOW_INFORMATION_INPUT)
                                .message(message)
                                .build()))
                .build();
    }
}