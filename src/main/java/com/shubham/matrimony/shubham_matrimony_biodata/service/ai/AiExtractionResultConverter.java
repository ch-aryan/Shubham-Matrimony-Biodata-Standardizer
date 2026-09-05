package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts validated AI semantic review completions, corrections, and attributes
 * into atomic {@link ExtractionResult} instances for downstream reconciliation.
 */
@Component
@RequiredArgsConstructor
public class AiExtractionResultConverter {

    private final GeminiResponseValidator validator;

    public List<ExtractionResult> convertToEvidence(AiSemanticReviewResult aiResult) {
        List<ExtractionResult> evidence = new ArrayList<>();
        if (aiResult == null) {
            return evidence;
        }

        // 1. Process completions
        if (aiResult.getCompletions() != null) {
            for (AiSemanticReviewResult.AiCompletion comp : aiResult.getCompletions()) {
                BiodataField field = validator.resolveBiodataField(comp.getField());
                if (field == null) continue;

                ExtractionContext context = resolveContext(comp.getContext(), field);
                FieldConfidence confidence = parseConfidence(comp.getConfidence());

                evidence.add(ExtractionResult.builder()
                        .field(field)
                        .value(comp.getValue().trim())
                        .context(context)
                        .confidence(confidence)
                        .method(ExtractionMethod.SEMANTIC_AI)
                        .sourceText(comp.getSourceSnippet())
                        .build());
            }
        }

        // 2. Process corrections
        if (aiResult.getCorrections() != null) {
            for (AiSemanticReviewResult.AiCorrection corr : aiResult.getCorrections()) {
                BiodataField field = validator.resolveBiodataField(corr.getField());
                if (field == null) continue;

                ExtractionContext context = resolveContext(null, field);

                evidence.add(ExtractionResult.builder()
                        .field(field)
                        .value(corr.getSuggestedValue().trim())
                        .context(context)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.SEMANTIC_AI)
                        .sourceText(corr.getSourceSnippet())
                        .build());
            }
        }

        // 3. Process additional attributes
        if (aiResult.getAdditionalAttributes() != null) {
            for (AiSemanticReviewResult.AiAdditionalAttribute attr : aiResult.getAdditionalAttributes()) {
                BiodataField field = validator.resolveBiodataField(attr.getKey());
                if (field == null) {
                    field = BiodataField.CUSTOM_ATTRIBUTE;
                }
                evidence.add(ExtractionResult.builder()
                        .field(field)
                        .value(attr.getValue().trim())
                        .context(ExtractionContext.OTHER)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.SEMANTIC_AI)
                        .sourceText(attr.getSourceSnippet())
                        .build());
            }
        }

        return evidence;
    }

    private ExtractionContext resolveContext(String contextStr, BiodataField field) {
        if (contextStr != null && !contextStr.isBlank()) {
            try {
                return ExtractionContext.valueOf(contextStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Infer from field if context string is missing or unrecognized
        if (field == BiodataField.FATHER_NAME || field == BiodataField.FATHER_OCCUPATION) {
            return ExtractionContext.FATHER;
        }
        if (field == BiodataField.MOTHER_NAME || field == BiodataField.MOTHER_OCCUPATION) {
            return ExtractionContext.MOTHER;
        }
        if (field == BiodataField.SIBLINGS) {
            return ExtractionContext.SIBLING;
        }
        if (field == BiodataField.PROPERTIES) {
            return ExtractionContext.PROPERTY;
        }
        return ExtractionContext.CANDIDATE;
    }

    private FieldConfidence parseConfidence(String confStr) {
        if (confStr == null || confStr.isBlank()) {
            return FieldConfidence.HIGH;
        }
        try {
            return FieldConfidence.valueOf(confStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FieldConfidence.HIGH;
        }
    }
}

