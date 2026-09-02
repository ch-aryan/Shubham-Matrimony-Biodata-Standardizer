package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

/**
 * Applies candidate-level labeled field segments that are not handled by any
 * of the specialized extractors ({@link FamilyExtractor}, {@link OccupationExtractor}, etc.).
 *
 * <p>Handles:
 * <ul>
 *   <li>{@code SURNAME} — buffered separately; merged with {@code givenName} in post-processing.</li>
 *   <li>{@code FULL_NAME} — buffered as {@code givenName}; longest-wins when seen multiple times.</li>
 *   <li>{@code SIBLINGS} — candidate-level sibling count/description at top level.</li>
 *   <li>All remaining fields — generic first-wins apply via
 *       {@link BiodataField#apply(com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata, String)}.</li>
 * </ul>
 *
 * <p>This extractor is the <em>last</em> step in the segment-dispatch chain and is only
 * called when {@link FamilyExtractor#tryApply} returned {@code false} (i.e. the segment
 * belongs to the candidate, not a family member).
 */
public class PropertyExtractor {

    /**
     * Applies a candidate-level segment to the profile stored in {@code ctx}.
     *
     * <p>Assumes the caller has already verified the segment is <em>not</em> a family field
     * (i.e. {@link FamilyExtractor#tryApply} returned {@code false}).
     *
     * @param field the canonical field enum constant
     * @param value the extracted string value
     * @param ctx   shared parse context
     */
    public void apply(BiodataField field, String value, ParseContext ctx) {

        // SURNAME is buffered and merged with givenName in post-processing
        if (field == BiodataField.SURNAME) {
            if (ctx.surname == null) {
                ctx.surname = value;
            }
            return;
        }

        // FULL_NAME: keep the longest value that contains the previous one (handles
        // "Aryan" followed by "Aryan Kumar" → prefer the more complete form)
        if (field == BiodataField.FULL_NAME) {
            if (ctx.givenName == null) {
                ctx.givenName = value;
            } else if (value.length() > ctx.givenName.length()
                    && value.toLowerCase().contains(ctx.givenName.toLowerCase())) {
                ctx.givenName = value;
            }
            return;
        }

        // Candidate-level siblings (e.g. "2 Brothers, 1 Sister")
        if (field == BiodataField.SIBLINGS) {
            ctx.siblingEntries.add(value);
            return;
        }

        // Generic first-wins: apply only if the field has not been set yet
        String currentVal = field.getGetter().apply(ctx.profile);
        if (currentVal == null || currentVal.isBlank()) {
            field.apply(ctx.profile, value);
        }
    }
}
