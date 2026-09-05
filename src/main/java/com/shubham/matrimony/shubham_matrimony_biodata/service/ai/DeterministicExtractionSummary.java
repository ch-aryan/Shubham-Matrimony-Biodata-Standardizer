package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the complete findings of the deterministic parsing engine
 * alongside raw usable text, serving as the input context for AI semantic review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeterministicExtractionSummary {

    private String rawText;
    private ProfileBiodata profile;

    @Builder.Default
    private Map<String, FieldConfidence> confidenceScores = new HashMap<>();

    @Builder.Default
    private List<ConflictRecord> conflicts = new ArrayList<>();

    @Builder.Default
    private List<ExtractionResult> evidenceTrail = new ArrayList<>();

    @Builder.Default
    private List<String> unparsedLines = new ArrayList<>();

    @Builder.Default
    private List<String> missingCanonicalFields = new ArrayList<>();
}

