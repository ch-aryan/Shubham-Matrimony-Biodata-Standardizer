package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates, filters, and sanitizes structured responses returned by AI.
 * Enforces field whitelisting, data presence, and evidence verification.
 */
@Slf4j
@Component
public class GeminiResponseValidator {

    private final ObjectMapper objectMapper;
    private final Set<String> allowedFieldKeys;

    public GeminiResponseValidator() {
        this.objectMapper = new ObjectMapper();
        this.allowedFieldKeys = new HashSet<>();
        for (BiodataField field : BiodataField.values()) {
            allowedFieldKeys.add(field.name().toLowerCase());
            allowedFieldKeys.add(field.getPropertyName().toLowerCase());
        }
    }

    /**
     * Validates and sanitizes raw JSON response text from Gemini.
     *
     * @param jsonResponse raw text containing JSON payload
     * @return validated result or empty if invalid
     */
    public Optional<AiSemanticReviewResult> validateAndSanitize(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            log.warn("Gemini response is null or blank");
            return Optional.empty();
        }

        AiSemanticReviewResult rawResult;
        try {
            rawResult = objectMapper.readValue(jsonResponse, AiSemanticReviewResult.class);
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response JSON into AiSemanticReviewResult: {}", e.getMessage());
            return Optional.empty();
        }

        if (rawResult == null) {
            return Optional.empty();
        }

        // Sanitize completions
        List<AiSemanticReviewResult.AiCompletion> validCompletions = new ArrayList<>();
        if (rawResult.getCompletions() != null) {
            for (AiSemanticReviewResult.AiCompletion comp : rawResult.getCompletions()) {
                if (isValidCompletion(comp)) {
                    comp.setField(canonicalizeFieldName(comp.getField()));
                    validCompletions.add(comp);
                } else {
                    log.debug("Discarding invalid AI completion: {}", comp);
                }
            }
        }

        // Sanitize corrections
        List<AiSemanticReviewResult.AiCorrection> validCorrections = new ArrayList<>();
        if (rawResult.getCorrections() != null) {
            for (AiSemanticReviewResult.AiCorrection corr : rawResult.getCorrections()) {
                if (isValidCorrection(corr)) {
                    corr.setField(canonicalizeFieldName(corr.getField()));
                    validCorrections.add(corr);
                } else {
                    log.debug("Discarding invalid AI correction: {}", corr);
                }
            }
        }

        // Sanitize conflict resolutions
        List<AiSemanticReviewResult.AiConflictResolution> validResolutions = new ArrayList<>();
        if (rawResult.getConflictResolutions() != null) {
            for (AiSemanticReviewResult.AiConflictResolution res : rawResult.getConflictResolutions()) {
                if (isValidConflictResolution(res)) {
                    res.setField(canonicalizeFieldName(res.getField()));
                    validResolutions.add(res);
                } else {
                    log.debug("Discarding invalid AI conflict resolution: {}", res);
                }
            }
        }

        // Sanitize additional attributes
        List<AiSemanticReviewResult.AiAdditionalAttribute> validAttributes = new ArrayList<>();
        if (rawResult.getAdditionalAttributes() != null) {
            for (AiSemanticReviewResult.AiAdditionalAttribute attr : rawResult.getAdditionalAttributes()) {
                if (isValidAdditionalAttribute(attr)) {
                    validAttributes.add(attr);
                }
            }
        }

        return Optional.of(AiSemanticReviewResult.builder()
                .completions(validCompletions)
                .corrections(validCorrections)
                .conflictResolutions(validResolutions)
                .additionalAttributes(validAttributes)
                .build());
    }

    private boolean isValidCompletion(AiSemanticReviewResult.AiCompletion comp) {
        if (comp == null || comp.getField() == null || comp.getValue() == null) {
            return false;
        }
        if (comp.getValue().isBlank() || comp.getField().isBlank()) {
            return false;
        }
        if (!isFieldWhitelisted(comp.getField())) {
            return false;
        }
        // Evidence requirement: must have sourceSnippet
        return comp.getSourceSnippet() != null && !comp.getSourceSnippet().isBlank();
    }

    private boolean isValidCorrection(AiSemanticReviewResult.AiCorrection corr) {
        if (corr == null || corr.getField() == null || corr.getSuggestedValue() == null) {
            return false;
        }
        if (corr.getField().isBlank() || corr.getSuggestedValue().isBlank()) {
            return false;
        }
        if (!isFieldWhitelisted(corr.getField())) {
            return false;
        }
        // Evidence requirement: must have sourceSnippet
        return corr.getSourceSnippet() != null && !corr.getSourceSnippet().isBlank();
    }

    private boolean isValidConflictResolution(AiSemanticReviewResult.AiConflictResolution res) {
        if (res == null || res.getField() == null || res.getRecommendedValue() == null) {
            return false;
        }
        if (res.getField().isBlank() || res.getRecommendedValue().isBlank()) {
            return false;
        }
        return isFieldWhitelisted(res.getField());
    }

    private boolean isValidAdditionalAttribute(AiSemanticReviewResult.AiAdditionalAttribute attr) {
        if (attr == null || attr.getKey() == null || attr.getValue() == null) {
            return false;
        }
        return !attr.getKey().isBlank() && !attr.getValue().isBlank();
    }

    public boolean isFieldWhitelisted(String fieldName) {
        if (fieldName == null) return false;
        return allowedFieldKeys.contains(fieldName.trim().toLowerCase());
    }

    public String canonicalizeFieldName(String rawFieldName) {
        if (rawFieldName == null) return null;
        String key = rawFieldName.trim().toLowerCase();
        for (BiodataField field : BiodataField.values()) {
            if (field.name().toLowerCase().equals(key) || field.getPropertyName().toLowerCase().equals(key)) {
                return field.getPropertyName();
            }
        }
        return rawFieldName;
    }

    public BiodataField resolveBiodataField(String fieldName) {
        if (fieldName == null) return null;
        String key = fieldName.trim().toLowerCase();
        for (BiodataField field : BiodataField.values()) {
            if (field.name().toLowerCase().equals(key) || field.getPropertyName().toLowerCase().equals(key)) {
                return field;
            }
        }
        return null;
    }
}

