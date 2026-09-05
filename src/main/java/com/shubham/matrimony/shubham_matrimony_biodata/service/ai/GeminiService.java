package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates Gemini AI semantic review, prompt building, transport execution,
 * and validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService implements AiExtractionProvider {

    private final GeminiConfigProperties config;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final GeminiResponseValidator validator;

    @Override
    public Optional<AiSemanticReviewResult> reviewAndComplete(DeterministicExtractionSummary summary) {
        if (!isAvailable()) {
            log.info("Gemini is not available (disabled or API key missing). Skipping review.");
            return Optional.empty();
        }

        try {
            String systemInstruction = promptBuilder.buildSystemInstruction();
            String userPrompt = promptBuilder.buildUserPrompt(summary);

            Optional<String> rawJsonOpt = geminiClient.generateJson(systemInstruction, userPrompt);
            if (rawJsonOpt.isEmpty()) {
                log.warn("Gemini client returned empty response (timeout, error, or rate limited)");
                return Optional.empty();
            }

            return validator.validateAndSanitize(rawJsonOpt.get());

        } catch (Exception e) {
            log.error("Unexpected error during Gemini reviewAndComplete: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Extracts text verbatim from image or scanned PDF bytes using Gemini
     * Multimodal.
     *
     * @param fileBytes raw file bytes
     * @param mimeType  MIME type of the document
     * @return extracted raw text, or empty if Gemini is unavailable or failed
     */
    public Optional<String> extractDocumentText(byte[] fileBytes, String mimeType) {
        if (!isAvailable()) {
            log.info("Gemini is not available (disabled or API key missing). Skipping document text extraction.");
            return Optional.empty();
        }
        return geminiClient.extractTextFromMultimodalDocument(fileBytes, mimeType);
    }

    @Override
    public boolean isAvailable() {
        return config.getApi().isEnabled()
                && config.getApi().getKey() != null
                && !config.getApi().getKey().isBlank();
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public String getModelName() {
        return config.getApi().getModel();
    }
}
