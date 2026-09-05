package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.EvidenceKey;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseRequest;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.EducationExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.InputQualityValidator;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ParseContext;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DeterministicFixesTest {

    private BiodataParserImplementation parser;
    private InputQualityValidator validator;
    private Validator beanValidator;

    @BeforeEach
    public void setUp() {
        parser = new BiodataParserImplementation();
        validator = new InputQualityValidator();
        beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void testP0_1_ConflictingEvidenceEmittedAndMerged() {
        String input = """
                Name: Rajesh Kumar
                DOB: 15-08-1995
                Occupation: Software Architect
                Profession: Senior Project Manager
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        assertNotNull(result);

        // Check evidence trail
        List<ExtractionResult> occEvidence = result.getEvidenceTrail().stream()
                .filter(e -> e.getField() == BiodataField.OCCUPATION
                        && e.getContext() == ExtractionContext.CANDIDATE)
                .toList();

        // Both occupations should be emitted
        assertTrue(occEvidence.size() >= 2,
                "Expected at least 2 occupation evidence items but was: " + occEvidence.size());

        // Conflict should be detected by ExtractionMerger
        assertNotNull(result.getConflicts());
        boolean hasOccConflict = result.getConflicts().stream()
                .anyMatch(c -> c.getKey().field() == BiodataField.OCCUPATION);
        assertTrue(hasOccConflict, "Occupation conflict should have been recorded");
    }

    @Test
    public void testP0_2_AdditionalInfoEvidenceHasNonNullableField() {
        String input = """
                Name: Deepa Sharma
                DOB: 20-05-1998
                Properties: 2 BHK Flat in Hyderabad
                Marital Status: Never Married
                Weight: 55 kg
                Complexion: Fair
                Diet: Vegetarian
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        assertNotNull(result.getEvidenceTrail());

        for (ExtractionResult item : result.getEvidenceTrail()) {
            assertNotNull(item.getField(), "Evidence item must not have a null BiodataField: " + item);
        }

        // Verify custom attribute provenance was recorded
        assertNotNull(result.getProfile().getAdditionalInfo());
        assertEquals("Vegetarian", result.getProfile().getAdditionalInfo().getCustomAttributes().get("Diet"));
        assertFalse(result.getProfile().getAdditionalInfo().getCustomAttributeDetails().isEmpty());
    }

    @Test
    public void testP0_2_EvidenceKeyNullFieldSafety() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenceKey(ExtractionContext.CANDIDATE, null));
    }

    @Test
    public void testP0_5_InputQualityValidatorWithConflictOnly() {
        Map<String, FieldConfidence> scores = new HashMap<>();
        scores.put("occupation", FieldConfidence.CONFLICT);
        scores.put("fullName", FieldConfidence.MISSING);

        // Should NOT be rejected as empty input
        ParseStatus status = validator.classify(scores, List.of());
        assertEquals(ParseStatus.SUCCESS_WITH_WARNINGS, status);

        List<ParseWarning> warnings = validator.generateWarnings(status, scores, List.of());
        assertTrue(warnings.stream().anyMatch(w -> w.getCategory() == WarningCategory.CONFLICT_DETECTED),
                "Should emit CONFLICT_DETECTED warning");
    }

    @Test
    public void testP0_5_ParseAndValidatePreservesProfileWhenConflictsExist() {
        String input = """
                Name: Swathi Rao
                Occupation: Data Engineer
                Occupation: Systems Analyst
                """;

        ParseResponse response = parser.parseAndValidate(input);
        assertNotEquals(ParseStatus.REJECTED_INPUT, response.getStatus());
        assertNotNull(response.getProfile(), "Profile must not be wiped to null when fields are recognized");
        assertEquals("Swathi Rao", response.getProfile().getFullName());
    }

    @Test
    public void testEducationArrayOpenRejectsNonQualificationBrackets() {
        EducationExtractor educationExtractor = new EducationExtractor();
        ParseContext ctx = new ParseContext();

        boolean handled = educationExtractor.tryDetectArrayOpen("hobbies: [", ctx);
        assertFalse(handled, "Non-qualification array brackets should return false");
        assertNull(ctx.inArrayField);

        boolean handledQual = educationExtractor.tryDetectArrayOpen("education: [", ctx);
        assertTrue(handledQual, "Qualification array brackets should return true");
        assertEquals(BiodataField.QUALIFICATION, ctx.inArrayField);
    }

    @Test
    public void testParseRequestBeanValidation() {
        // Blank rawText
        ParseRequest blankReq = ParseRequest.builder().rawText("   ").build();
        Set<ConstraintViolation<ParseRequest>> violations = beanValidator.validate(blankReq);
        assertFalse(violations.isEmpty(), "Blank rawText must violate @NotBlank");

        // Oversized rawText (>50K)
        String oversized = "a".repeat(50_001);
        ParseRequest overReq = ParseRequest.builder().rawText(oversized).build();
        Set<ConstraintViolation<ParseRequest>> overViolations = beanValidator.validate(overReq);
        assertFalse(overViolations.isEmpty(), "Oversized rawText must violate @Size");

        // Valid rawText
        ParseRequest validReq = ParseRequest.builder().rawText("Name: Amit").build();
        Set<ConstraintViolation<ParseRequest>> validViolations = beanValidator.validate(validReq);
        assertTrue(validViolations.isEmpty(), "Valid rawText must pass validation");
    }
}
