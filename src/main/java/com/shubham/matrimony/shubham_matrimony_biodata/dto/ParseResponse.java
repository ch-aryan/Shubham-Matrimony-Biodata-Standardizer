package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified response contract for the biodata parsing API.
 *
 * <p>
 * Contains:
 * <ul>
 * <li>{@link #status} — overall outcome classification.</li>
 * <li>{@link #profile} — the extracted biodata (null when
 * {@code REJECTED_INPUT}).</li>
 * <li>{@link #confidenceScores} — HIGH or MISSING per canonical field.</li>
 * <li>{@link #warnings} — categorized warnings for the frontend/operator.</li>
 * <li>{@link #unparsedLines} — all lines the parser could not map
 * (untruncated).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {

    private ParseStatus status;

    private ProfileBiodata profile;

    @Builder.Default
    private Map<String, FieldConfidence> confidenceScores = new HashMap<>();

    @Builder.Default
    private List<ConflictRecord> conflicts = new ArrayList<>();

    @Builder.Default
    private List<ExtractionResult> evidenceTrail = new ArrayList<>();

    @Builder.Default
    private List<ParseWarning> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> unparsedLines = new ArrayList<>();

    private AiReviewMetadata aiMetadata;
}
