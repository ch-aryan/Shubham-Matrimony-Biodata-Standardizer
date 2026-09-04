package com.shubham.matrimony.shubham_matrimony_biodata.dto;

/**
 * Confidence level of an extracted piece of evidence or canonical field score.
 *
 * <ul>
 *   <li>{@link #HIGH} — verified deterministic or high-certainty extraction.</li>
 *   <li>{@link #MEDIUM} — heuristic or fuzzy match that may warrant quick operator verification.</li>
 *   <li>{@link #LOW} — weak match; likely needs human review.</li>
 *   <li>{@link #CONFLICT} — multiple competing values discovered for the same field.</li>
 *   <li>{@link #MISSING} — field was not found in the input biodata.</li>
 * </ul>
 */
public enum FieldConfidence {
    HIGH,
    MEDIUM,
    LOW,
    CONFLICT,
    MISSING
}
