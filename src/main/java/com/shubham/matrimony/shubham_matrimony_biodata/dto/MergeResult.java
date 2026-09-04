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
 * The output of {@code ExtractionMerger}, encapsulating the synthesized
 * {@link ProfileBiodata},
 * the field-level confidence scores, all detected conflicts, and the complete
 * audit trail
 * of underlying evidence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult {

    /** The canonical profile populated from reconciled evidence. */
    @Builder.Default
    private ProfileBiodata profile = new ProfileBiodata();

    /**
     * Confidence level per canonical field property name (HIGH, MEDIUM, LOW,
     * CONFLICT, MISSING).
     */
    @Builder.Default
    private Map<String, FieldConfidence> confidenceScores = new HashMap<>();

    /** All contradictory evidence sets that warrant operator attention. */
    @Builder.Default
    private List<ConflictRecord> conflicts = new ArrayList<>();

    /** Complete audit trail of all evidence bucketed by (Context, Field). */
    @Builder.Default
    private Map<EvidenceKey, List<ExtractionResult>> evidenceTrail = new HashMap<>();

    /**
     * Checks if any conflicts were detected during merging.
     *
     * @return {@code true} if at least one conflict was recorded; {@code false}
     *         otherwise.
     */
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }
}
