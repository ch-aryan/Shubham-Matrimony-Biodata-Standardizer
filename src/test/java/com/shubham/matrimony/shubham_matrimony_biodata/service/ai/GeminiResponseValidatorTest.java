package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeminiResponseValidatorTest {

    private GeminiResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GeminiResponseValidator();
    }

    @Test
    void testValidJsonResponseIsSanitizedAndAccepted() {
        String json = """
            {
              "corrections": [
                {
                  "field": "fullName",
                  "originalValue": "Rohan",
                  "suggestedValue": "Rohan Thota",
                  "sourceSnippet": "Name: Rohan Thota",
                  "rationale": "Full surname present in document"
                }
              ],
              "completions": [
                {
                  "field": "salary",
                  "context": "CANDIDATE",
                  "value": "CAD 100K",
                  "confidence": "HIGH",
                  "sourceSnippet": "Package: CAD 100K",
                  "rationale": "Found in career section"
                }
              ],
              "conflictResolutions": [
                {
                  "field": "occupation",
                  "context": "CANDIDATE",
                  "recommendedValue": "Assistant Manager",
                  "rationale": "Directly stated on line 12"
                }
              ],
              "additionalAttributes": [
                {
                  "key": "habits",
                  "value": "Non-smoker, Vegetarian",
                  "sourceSnippet": "Habits: Non-smoker, Vegetarian"
                }
              ]
            }
            """;

        Optional<AiSemanticReviewResult> resultOpt = validator.validateAndSanitize(json);
        assertTrue(resultOpt.isPresent());
        AiSemanticReviewResult result = resultOpt.get();

        assertEquals(1, result.getCorrections().size());
        assertEquals("fullName", result.getCorrections().get(0).getField());
        assertEquals("Rohan Thota", result.getCorrections().get(0).getSuggestedValue());

        assertEquals(1, result.getCompletions().size());
        assertEquals("salary", result.getCompletions().get(0).getField());
        assertEquals("CAD 100K", result.getCompletions().get(0).getValue());

        assertEquals(1, result.getConflictResolutions().size());
        assertEquals("occupation", result.getConflictResolutions().get(0).getField());

        assertEquals(1, result.getAdditionalAttributes().size());
    }

    @Test
    void testDiscardsCompletionsWithoutSourceSnippet() {
        String json = """
            {
              "completions": [
                {
                  "field": "salary",
                  "context": "CANDIDATE",
                  "value": "CAD 100K",
                  "sourceSnippet": "",
                  "rationale": "Just guessing"
                }
              ]
            }
            """;

        Optional<AiSemanticReviewResult> resultOpt = validator.validateAndSanitize(json);
        assertTrue(resultOpt.isPresent());
        assertTrue(resultOpt.get().getCompletions().isEmpty(), "Completion without sourceSnippet must be discarded");
    }

    @Test
    void testDiscardsUnrecognizedFields() {
        String json = """
            {
              "completions": [
                {
                  "field": "favorite_superhero",
                  "context": "CANDIDATE",
                  "value": "Batman",
                  "sourceSnippet": "Hero: Batman",
                  "rationale": "Found in hobbies"
                }
              ]
            }
            """;

        Optional<AiSemanticReviewResult> resultOpt = validator.validateAndSanitize(json);
        assertTrue(resultOpt.isPresent());
        assertTrue(resultOpt.get().getCompletions().isEmpty(), "Unrecognized field must be discarded from completions");
    }

    @Test
    void testInvalidJsonReturnsEmptyOptional() {
        String invalidJson = "{ not valid json ...";
        Optional<AiSemanticReviewResult> resultOpt = validator.validateAndSanitize(invalidJson);
        assertFalse(resultOpt.isPresent());
    }

    @Test
    void testBlankOrNullReturnsEmptyOptional() {
        assertFalse(validator.validateAndSanitize(null).isPresent());
        assertFalse(validator.validateAndSanitize("   ").isPresent());
    }
}

