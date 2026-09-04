package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single atomic piece of extracted evidence from a biodata.
 *
 * <p>
 * Rather than directly mutating {@link ProfileBiodata}, extractors emit
 * {@code ExtractionResult} instances. Downstream, an {@code ExtractionMerger}
 * collects all evidence, resolves conflicts, and produces the canonical
 * profile.
 *
 * <p>
 * Example:
 * 
 * <pre>{@code
 * ExtractionResult evidence = ExtractionResult.builder()
 *         .field(BiodataField.FULL_NAME)
 *         .value("Satwik Kotte")
 *         .context(ExtractionContext.CANDIDATE)
 *         .confidence(FieldConfidence.HIGH)
 *         .method(ExtractionMethod.DETERMINISTIC)
 *         .sourceText("Name: Satwik Kotte")
 *         .build();
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    /** The canonical biodata field this evidence relates to. */
    private BiodataField field;

    /** The extracted value. */
    private String value;

    /**
     * The entity/scope this information belongs to (e.g. CANDIDATE, FATHER,
     * SIBLING).
     */
    private ExtractionContext context;

    /** The certainty level of this extraction. */
    private FieldConfidence confidence;

    /**
     * The technique used to extract this value (e.g. DETERMINISTIC, FUZZY,
     * SEMANTIC_AI).
     */
    private ExtractionMethod method;

    /** The original raw line or snippet from which this value was derived. */
    private String sourceText;
}
