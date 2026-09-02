package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataLabels;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils;

/**
 * Handles qualification / education extraction for three distinct scenarios:
 *
 * <ol>
 *   <li>{@link #tryExtractArrayItem} — consumes continuation lines inside an open
 *       multi-line JSON education array (e.g. {@code "education": ["B.Tech", "PGD…"]}).</li>
 *   <li>{@link #tryExtractStandalone} — detects standalone degree-prefix lines
 *       (e.g. {@code "B.Tech JNTU"}, {@code "MBA IIM"}) that carry no label.</li>
 *   <li>{@link #tryDetectArrayOpen} — detects the opening bracket of a multi-line
 *       qualification array and sets {@code ctx.inArrayField}.</li>
 * </ol>
 */
public class EducationExtractor {

    // ── 1. Multi-line JSON array item continuation ────────────────────────────

    /**
     * If the parser is currently inside a multi-line education array, consumes this
     * line as an array item and appends it to the qualification field.
     *
     * <p>Exits the array mode (without consuming the line) when the line contains
     * {@code ]}, {@code {}, or {@code :} — these signal the array is closing or a new
     * structure is starting.
     *
     * @return {@code true} if the line was consumed as an array item; caller should
     *         {@code continue}. {@code false} means normal processing should proceed.
     */
    public boolean tryExtractArrayItem(String sanitized, ParseContext ctx) {
        if (ctx.inArrayField == null) return false;

        // Termination: closing bracket, new object, or a labeled key
        if (sanitized.contains("]") || sanitized.contains("{") || sanitized.contains(":")) {
            ctx.inArrayField = null;
            return false; // let the line be processed normally
        }

        String item = BiodataParserUtils.cleanValue(sanitized);
        if (!item.isBlank() && !item.equals(",")) {
            String existing = ctx.profile.getQualification();
            if (existing == null || existing.isBlank()) {
                ctx.profile.setQualification(item);
            } else if (!existing.contains(item)) {
                ctx.profile.setQualification(existing + ", " + item);
            }
        }
        return true; // consumed — skip the rest of this line
    }

    // ── 2. Standalone degree-prefix line ─────────────────────────────────────

    /**
     * Detects a standalone education degree prefix line (no label, no colon, no comma).
     *
     * <p><b>Examples:</b> {@code "B.Tech JNTU"}, {@code "MBA IIM"},
     * {@code "PGD in Financial Planning"}.
     *
     * <p>Does nothing inside a family block (education lines there are not the candidate's).
     *
     * @return {@code true} if the line was consumed as a qualification; caller should
     *         {@code continue}.
     */
    public boolean tryExtractStandalone(String sanitized, ParseContext ctx) {
        if (ctx.inFamilyBlock || sanitized.contains(":") || sanitized.contains(",")) {
            return false;
        }
        String lower = sanitized.toLowerCase();
        if (lower.startsWith("b.tech") || lower.startsWith("m.tech") || lower.startsWith("bba")
                || lower.startsWith("mba")     || lower.startsWith("ms ")   || lower.startsWith("mbbs")
                || lower.startsWith("b.sc")    || lower.startsWith("b.com") || lower.startsWith("be ")
                || lower.startsWith("b.e.")    || lower.startsWith("diploma")
                || lower.startsWith("degree")  || lower.startsWith("pgd")
                || lower.startsWith("pg diploma")) {
            if (ctx.profile.getQualification() == null || ctx.profile.getQualification().isBlank()) {
                ctx.profile.setQualification(sanitized);
            } else if (!ctx.profile.getQualification().contains(sanitized)) {
                ctx.profile.setQualification(ctx.profile.getQualification() + ", " + sanitized);
            }
            return true;
        }
        return false;
    }

    // ── 3. Array-open bracket detection ──────────────────────────────────────

    /**
     * Detects a line that opens a multi-line qualification array
     * (e.g. {@code "education": [} or {@code "విద్య": [}).
     *
     * <p>Sets {@code ctx.inArrayField = QUALIFICATION} when a known qualification alias
     * appears before the {@code [} bracket so subsequent lines are collected as array items.
     *
     * @return {@code true} if the line was an array-open bracket (should be skipped);
     *         caller should {@code continue}.
     */
    public boolean tryDetectArrayOpen(String sanitized, ParseContext ctx) {
        if (!sanitized.contains("[") || sanitized.contains("]") || sanitized.contains("{")) {
            return false;
        }
        String beforeBracket = sanitized.substring(0, sanitized.indexOf('['));
        for (String alias : BiodataLabels.QUALIFICATION) {
            if (beforeBracket.toLowerCase().contains(alias)) {
                ctx.inArrayField = BiodataField.QUALIFICATION;
                break;
            }
        }
        return true; // always skip the bracket-open line itself
    }
}
