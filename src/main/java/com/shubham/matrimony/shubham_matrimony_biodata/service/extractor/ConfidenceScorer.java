package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

import java.util.Map;

/**
 * Post-processing phase executed after the main parse loop completes.
 *
 * <p>
 * Responsibilities (in order):
 * <ol>
 * <li>Flush the last buffered sibling record (the loop exit may leave one
 * pending).</li>
 * <li>Join all sibling entries into {@code siblingsDetails} if not already
 * set.</li>
 * <li>Merge {@code surname} and {@code givenName} buffers into
 * {@code fullName}.</li>
 * <li>Compute a {@link FieldConfidence} map: HIGH when a field value is
 * present,
 * MISSING otherwise.</li>
 * </ol>
 *
 * <p>
 * Also provides {@link #populateMissingConfidence} used as a fast-path when the
 * raw input is blank or null.
 * 
 * @deprecated Replaced by {@link ProfileFinalizer} and
 *             {@link ExtractionMerger}.
 *             Authoritative confidence scoring is handled by
 *             {@link ExtractionMerger}.
 */
@Deprecated
public class ConfidenceScorer {

    private final FamilyExtractor familyExtractor;
    private final ProfileFinalizer profileFinalizer;

    /**
     * @param familyExtractor used to flush the last sibling record
     */
    public ConfidenceScorer(FamilyExtractor familyExtractor) {
        this.familyExtractor = familyExtractor;
        this.profileFinalizer = new ProfileFinalizer(familyExtractor);
    }

    /**
     * Finalizes the profile in {@code ctx} and populates {@code confidenceScores}.
     *
     * @param ctx              shared parse context (mutated: fullName,
     *                         siblingsDetails may be set)
     * @param confidenceScores target map to fill with HIGH / MISSING per field
     */
    public ConfidenceScorer(ProfileFinalizer profileFinalizer) {
        this.familyExtractor = null;
        this.profileFinalizer = profileFinalizer;
    }

    public void finalizeProfile(ParseContext ctx, Map<String, FieldConfidence> confidenceScores) {

        // Flush the last sibling record that the loop may have left pending
        familyExtractor.flushCurrentSibling(ctx);

        // Aggregate all sibling entries into a single comma-separated string
        if ((ctx.profile.getSiblingsDetails() == null || ctx.profile.getSiblingsDetails().isBlank())
                && !ctx.siblingEntries.isEmpty()) {
            ctx.profile.setSiblingsDetails(String.join(", ", ctx.siblingEntries));
        }

        // Merge surname + givenName into fullName
        // Examples:
        // surname="Thota" givenName="Rohan" → "Thota Rohan"
        // surname="Thota" givenName="Rohan Thota" → "Rohan Thota" (surname already
        // inside)
        // givenName="Aryan Kumar" (no surname) → "Aryan Kumar"
        // surname="Kamma" (no givenName) → "Kamma"
        if (ctx.surname != null && !ctx.surname.isBlank()
                && ctx.givenName != null && !ctx.givenName.isBlank()) {
            if (!ctx.givenName.toLowerCase().contains(ctx.surname.toLowerCase())) {
                ctx.profile.setFullName(ctx.surname + " " + ctx.givenName);
            } else {
                ctx.profile.setFullName(ctx.givenName);
            }
        } else if (ctx.givenName != null && !ctx.givenName.isBlank()) {
            ctx.profile.setFullName(ctx.givenName);
        } else if (ctx.surname != null && !ctx.surname.isBlank()) {
            ctx.profile.setFullName(ctx.surname);
        }

        // Compute confidence scores for every canonical field
        for (BiodataField field : BiodataField.values()) {
            if (field == BiodataField.SURNAME) {
                // SURNAME is an internal helper — it surfaces under fullName
                continue;
            }
            String val = field.getGetter().apply(ctx.profile);
            if (val != null && !val.isBlank()) {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.HIGH);
            } else {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.MISSING);
            }
        }
        profileFinalizer.finalizeProfile(ctx);
    }

    /**
     * Fast-path: marks every canonical field as {@link FieldConfidence#MISSING}.
     * Called when the raw input is null or blank — no parsing takes place.
     *
     * @param confidenceScores target map to fill
     */
    public void populateMissingConfidence(Map<String, FieldConfidence> confidenceScores) {
        for (BiodataField field : BiodataField.values()) {
            if (field != BiodataField.SURNAME) {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.MISSING);
            }
        }
        profileFinalizer.populateMissingConfidence(confidenceScores);
    }
}
