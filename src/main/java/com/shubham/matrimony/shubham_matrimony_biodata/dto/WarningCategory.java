package com.shubham.matrimony.shubham_matrimony_biodata.dto;

/**
 * Categories of warnings produced during parse result analysis.
 *
 * <ul>
 * <li>{@link #UNRECOGNIZED_TEXT} — lines the parser could not map to any
 * standard biodata field. These appear in {@code unparsedLines}.</li>
 * <li>{@link #MISSING_EXPECTED_INFORMATION} — informational: fewer fields
 * than expected were recognized. This is <em>not</em> an error — the
 * system is designed for human completion.</li>
 * <li>{@link #LOW_INFORMATION_INPUT} — zero fields were extracted. Triggers
 * {@link ParseStatus#REJECTED_INPUT}.</li>
 * </ul>
 */
public enum WarningCategory {
    UNRECOGNIZED_TEXT,
    MISSING_EXPECTED_INFORMATION,
    LOW_INFORMATION_INPUT,
    CONFLICT_DETECTED
    LOW_INFORMATION_INPUT
}
