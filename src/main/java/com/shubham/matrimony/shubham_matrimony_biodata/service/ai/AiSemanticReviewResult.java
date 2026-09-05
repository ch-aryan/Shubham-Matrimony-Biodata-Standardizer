package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured schema returned by AI semantic review containing evidence-backed
 * corrections, completions, conflict resolutions, and additional attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSemanticReviewResult {

    @Builder.Default
    private List<AiCorrection> corrections = new ArrayList<>();

    @Builder.Default
    private List<AiCompletion> completions = new ArrayList<>();

    @Builder.Default
    private List<AiConflictResolution> conflictResolutions = new ArrayList<>();

    @Builder.Default
    private List<AiAdditionalAttribute> additionalAttributes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiCorrection {
        private String field;
        private String originalValue;
        private String suggestedValue;
        private String sourceSnippet;
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiCompletion {
        private String field;
        private String context; // e.g. "CANDIDATE", "FATHER", "MOTHER", "SIBLING"
        private String value;
        private String confidence; // "HIGH", "MEDIUM", etc.
        private String sourceSnippet;
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiConflictResolution {
        private String field;
        private String context;
        private String recommendedValue;
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiAdditionalAttribute {
        private String key;
        private String value;
        private String sourceSnippet;
    }
}

