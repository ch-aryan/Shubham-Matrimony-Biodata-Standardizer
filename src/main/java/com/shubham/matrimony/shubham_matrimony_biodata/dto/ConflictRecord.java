package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Details of a detected contradiction where multiple non-identical evidence
 * items
 * were found for the same {@link EvidenceKey}.
 *
 * <p>
 * Preserves both the currently resolved primary value and all competing
 * alternatives
 * so a human operator can quickly verify or pick an alternative in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictRecord {

    /** The context and field where the contradiction occurred. */
    private EvidenceKey key;

    /** The primary value chosen for the profile field. */
    private String resolvedValue;

    /** All distinct competing values observed for this key. */
    @Builder.Default
    private List<String> competingValues = new ArrayList<>();

    /**
     * The full list of conflicting evidence objects with source text and
     * provenance.
     */
    @Builder.Default
    private List<ExtractionResult> competingEvidence = new ArrayList<>();

    /** Optional AI-recommended resolution value. */
    private String recommendedValue;

    /** Optional rationale behind the AI recommendation. */
    private String recommendationRationale;
}
