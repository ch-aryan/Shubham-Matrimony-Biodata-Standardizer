package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

/**
 * Composite key pairing an {@link ExtractionContext} (entity ownership) with a
 * {@link BiodataField} (attribute type).
 *
 * <p>By bucketing evidence under {@code (context, field)}, data belonging to different entities
 * (e.g. candidate's name vs sibling's name vs father's name) are completely isolated and
 * will never overwrite each other or cause false conflicts.
 *
 * @param context the entity scope (e.g. CANDIDATE, FATHER, MOTHER, SIBLING)
 * @param field   the canonical field type (e.g. FULL_NAME, OCCUPATION, DATE_OF_BIRTH)
 */
public record EvidenceKey(ExtractionContext context, BiodataField field) {

    public EvidenceKey {
        if (context == null) {
            context = ExtractionContext.ROOT;
        }
    }
}
