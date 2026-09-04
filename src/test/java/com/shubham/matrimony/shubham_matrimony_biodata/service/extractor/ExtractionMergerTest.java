package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.*;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExtractionMergerTest {

    private ExtractionMerger merger;

    @BeforeEach
    public void setUp() {
        merger = new ExtractionMerger();
    }

    @Test
    public void testSatwikProblem_ContextIsolation() {
        // Satwik's biodata contains "Name:" multiple times in different contexts:
        // 1. Candidate Name: Satwik Kotte
        // 2. Father Name: Sri Kotte Srisailam
        // 3. Sibling Name: Kotte Sahithi
        // 4. Sibling Spouse Name: Akula Vinayak
        List<ExtractionResult> evidence = List.of(
                ExtractionResult.builder()
                        .field(BiodataField.FULL_NAME)
                        .value("Satwik Kotte")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Name: Satwik Kotte")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.FULL_NAME)
                        .value("Sri Kotte Srisailam")
                        .context(ExtractionContext.FATHER)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Father's Name: Sri Kotte Srisailam")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.FULL_NAME)
                        .value("Kotte Sahithi")
                        .context(ExtractionContext.SIBLING)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Name: Kotte Sahithi")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.FULL_NAME)
                        .value("Akula Vinayak")
                        .context(ExtractionContext.SIBLING_SPOUSE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Spouse Name: Akula Vinayak")
                        .build()
        );

        MergeResult result = merger.merge(evidence);
        ProfileBiodata profile = result.getProfile();

        // Candidate name is preserved without being overwritten by sister, father, or brother-in-law!
        assertEquals("Satwik Kotte", profile.getFullName());
        assertEquals("Sri Kotte Srisailam", profile.getFatherName());
        assertTrue(profile.getSiblingsDetails().contains("Kotte Sahithi"));
        // No conflicts because they belong to completely different contexts!
        assertFalse(result.hasConflicts());
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fullName"));
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fatherName"));
    }

    @Test
    public void testConflictDetection_CompromisingDOB() {
        // Two conflicting DOBs found for the candidate
        List<ExtractionResult> evidence = List.of(
                ExtractionResult.builder()
                        .field(BiodataField.DATE_OF_BIRTH)
                        .value("19-11-1996")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("DOB: 19-11-1996")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.DATE_OF_BIRTH)
                        .value("21-11-1996")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.MEDIUM)
                        .method(ExtractionMethod.SEMANTIC_AI)
                        .sourceText("Date of Birth: 21-11-1996")
                        .build()
        );

        MergeResult result = merger.merge(evidence);

        // 1. A primary resolution is chosen (HIGH confidence wins)
        assertEquals("19-11-1996", result.getProfile().getDateOfBirth());
        // 2. But CONFLICT is explicitly flagged
        assertTrue(result.hasConflicts());
        assertEquals(1, result.getConflicts().size());

        ConflictRecord conflict = result.getConflicts().get(0);
        assertEquals(BiodataField.DATE_OF_BIRTH, conflict.getKey().field());
        assertEquals(ExtractionContext.CANDIDATE, conflict.getKey().context());
        assertEquals("19-11-1996", conflict.getResolvedValue());
        assertTrue(conflict.getCompetingValues().contains("19-11-1996"));
        assertTrue(conflict.getCompetingValues().contains("21-11-1996"));

        // Field confidence score is CONFLICT
        assertEquals(FieldConfidence.CONFLICT, result.getConfidenceScores().get("dateOfBirth"));
    }

    @Test
    public void testValueDeduplication() {
        // Two sources agree on the exact same company name
        List<ExtractionResult> evidence = List.of(
                ExtractionResult.builder()
                        .field(BiodataField.COMPANY)
                        .value("Salesforce")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Company Name: Salesforce")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.COMPANY)
                        .value("Salesforce")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.SEMANTIC_AI)
                        .sourceText("working at Salesforce")
                        .build()
        );

        MergeResult result = merger.merge(evidence);

        assertEquals("Salesforce", result.getProfile().getCompany());
        assertFalse(result.hasConflicts());
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("company"));
    }

    @Test
    public void testAccumulativeQualifications() {
        List<ExtractionResult> evidence = List.of(
                ExtractionResult.builder()
                        .field(BiodataField.QUALIFICATION)
                        .value("B.Tech")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Education: B.Tech")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.QUALIFICATION)
                        .value("MS Computer Science")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("MS Computer Science")
                        .build()
        );

        MergeResult result = merger.merge(evidence);

        // Accumulates both degrees rather than treating as conflict
        assertEquals("B.Tech, MS Computer Science", result.getProfile().getQualification());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void testCompositeNameAssembly() {
        List<ExtractionResult> evidence = List.of(
                ExtractionResult.builder()
                        .field(BiodataField.SURNAME)
                        .value("Kalakoti")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Surname: KALAKOTI")
                        .build(),
                ExtractionResult.builder()
                        .field(BiodataField.FULL_NAME)
                        .value("Sai Charan")
                        .context(ExtractionContext.CANDIDATE)
                        .confidence(FieldConfidence.HIGH)
                        .method(ExtractionMethod.DETERMINISTIC)
                        .sourceText("Name: SAI CHARAN")
                        .build()
        );

        MergeResult result = merger.merge(evidence);

        assertEquals("Kalakoti Sai Charan", result.getProfile().getFullName());
        assertFalse(result.hasConflicts());
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fullName"));
    }
}
