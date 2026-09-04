package com.shubham.matrimony.shubham_matrimony_biodata.dto;

/**
 * Identifies the provenance/mechanism by which a piece of evidence was
 * extracted.
 *
 * <ul>
 * <li>{@link #DETERMINISTIC} — extracted via exact label matching, regex, or
 * static rules.</li>
 * <li>{@link #FUZZY} — extracted via edit distance / transliteration spelling
 * similarity.</li>
 * <li>{@link #SEMANTIC_AI} — extracted via natural language understanding (e.g.
 * LLM).</li>
 * <li>{@link #USER} — entered or confirmed by a human operator in the
 * verification UI.</li>
 * </ul>
 */
public enum ExtractionMethod {
    DETERMINISTIC,
    FUZZY,
    SEMANTIC_AI,
    USER
}
