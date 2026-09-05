package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResultDTO {

    private ProfileBiodata profile;

    @Builder.Default
    private Map<String, FieldConfidence> confidenceScores = new HashMap<>();

    @Builder.Default
    private List<String> unparsedLines = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<ConflictRecord> conflicts = new ArrayList<>();

    @Builder.Default
    private List<ExtractionResult> evidenceTrail = new ArrayList<>();

    private AiReviewMetadata aiMetadata;

    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }
}
