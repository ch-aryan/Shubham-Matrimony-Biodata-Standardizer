package com.shubham.matrimony.shubham_matrimony_biodata.dto;

/**
 * Classification of the overall parse outcome.
 *
 * <ul>
 *   <li>{@link #SUCCESS} — at least one field extracted, no unparsed lines.</li>
 *   <li>{@link #SUCCESS_WITH_WARNINGS} — at least one field extracted,
 *       but some lines could not be mapped to standard fields.</li>
 *   <li>{@link #REJECTED_INPUT} — zero fields extracted; the supplied text
 *       does not appear to contain a supported biodata format.</li>
 * </ul>
 */
public enum ParseStatus {
    SUCCESS,
    SUCCESS_WITH_WARNINGS,
    REJECTED_INPUT
}
