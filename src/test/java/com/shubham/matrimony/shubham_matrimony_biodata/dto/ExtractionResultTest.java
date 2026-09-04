package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExtractionResultTest {

    @Test
    public void testExtractionResultBuilder() {
        ExtractionResult evidence = ExtractionResult.builder()
                .field(BiodataField.FULL_NAME)
                .value("Satwik Kotte")
                .context(ExtractionContext.CANDIDATE)
                .confidence(FieldConfidence.HIGH)
                .method(ExtractionMethod.DETERMINISTIC)
                .sourceText("Name: Satwik Kotte")
                .build();

        assertEquals(BiodataField.FULL_NAME, evidence.getField());
        assertEquals("Satwik Kotte", evidence.getValue());
        assertEquals(ExtractionContext.CANDIDATE, evidence.getContext());
        assertEquals(FieldConfidence.HIGH, evidence.getConfidence());
        assertEquals(ExtractionMethod.DETERMINISTIC, evidence.getMethod());
        assertEquals("Name: Satwik Kotte", evidence.getSourceText());
    }

    @Test
    public void testConflictConfidenceLevel() {
        ExtractionResult conflictingEvidence = ExtractionResult.builder()
                .field(BiodataField.DATE_OF_BIRTH)
                .value("21-11-1996")
                .context(ExtractionContext.CANDIDATE)
                .confidence(FieldConfidence.CONFLICT)
                .method(ExtractionMethod.DETERMINISTIC)
                .sourceText("Date of Birth: 21-11-1996")
                .build();

        assertEquals(FieldConfidence.CONFLICT, conflictingEvidence.getConfidence());
    }
}
