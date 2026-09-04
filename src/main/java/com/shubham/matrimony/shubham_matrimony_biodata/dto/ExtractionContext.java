package com.shubham.matrimony.shubham_matrimony_biodata.dto;

/**
 * Represents the ownership and scope of extracted information in a biodata.
 *
 * <p>
 * Identifies WHO or WHAT entity a piece of information belongs to.
 * This separates field identification from entity ownership (e.g.
 * distinguishing
 * a {@code Name:} belonging to {@link #CANDIDATE} vs {@link #SIBLING} vs
 * {@link #FATHER}).
 */
public enum ExtractionContext {
    ROOT,
    CANDIDATE,
    EDUCATION,
    CAREER,
    FAMILY,
    FATHER,
    MOTHER,
    SIBLING,
    SIBLING_SPOUSE,
    GRANDPARENTS,
    PROPERTY,
    OTHER
}
