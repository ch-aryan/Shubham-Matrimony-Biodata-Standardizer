package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.*;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataParserImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeminiReconciliationIntegrationTest {

    private GeminiConfigProperties config;
    private GeminiRoutingPolicy routingPolicy;
    private GeminiResponseValidator validator;
    private AiExtractionResultConverter converter;

    @BeforeEach
    void setUp() {
        config = new GeminiConfigProperties();
        config.getApi().setEnabled(true);
        config.getApi().setKey("mock-key");
        config.getRouting().setMinFieldsThreshold(5);

        routingPolicy = new GeminiRoutingPolicy(config);
        validator = new GeminiResponseValidator();
        converter = new AiExtractionResultConverter(validator);
    }

    @Test
    void testDetHighAndAiHighSameValueReinforcedHigh() {
        AiExtractionProvider mockProvider = new AiExtractionProvider() {
            @Override
            public Optional<AiSemanticReviewResult> reviewAndComplete(DeterministicExtractionSummary summary) {
                return Optional.of(AiSemanticReviewResult.builder()
                        .completions(List.of(
                                AiSemanticReviewResult.AiCompletion.builder()
                                        .field("fullName")
                                        .context("CANDIDATE")
                                        .value("Satwik Kotte")
                                        .confidence("HIGH")
                                        .sourceSnippet("Name: Satwik Kotte")
                                        .rationale("Matches document")
                                        .build()
                        ))
                        .build());
            }

            @Override
            public boolean isAvailable() { return true; }
            @Override
            public String getProviderName() { return "MockAI"; }
            @Override
            public String getModelName() { return "mock-model"; }
        };

        BiodataParserImplementation parser = new BiodataParserImplementation(mockProvider, routingPolicy, converter);

        String rawText = """
                Name: Satwik Kotte
                DOB: 15-08-1995
                Job: Software Engineer
                Caste: Brahmin
                Education: B.Tech
                """;

        ParseResponse response = parser.parseAndValidate(rawText, true);

        assertEquals("Satwik Kotte", response.getProfile().getFullName());
        assertEquals(FieldConfidence.HIGH, response.getConfidenceScores().get("fullName"));
        assertTrue(response.getConflicts().isEmpty(), "No conflict when deterministic and AI agree");
        assertNotNull(response.getAiMetadata());
        assertTrue(response.getAiMetadata().isInvoked());
    }

    @Test
    void testDetHighAndAiHighDifferingValuesCreatesConflictWithRecommendation() {
        AiExtractionProvider mockProvider = new AiExtractionProvider() {
            @Override
            public Optional<AiSemanticReviewResult> reviewAndComplete(DeterministicExtractionSummary summary) {
                return Optional.of(AiSemanticReviewResult.builder()
                        .corrections(List.of(
                                AiSemanticReviewResult.AiCorrection.builder()
                                        .field("occupation")
                                        .originalValue("Software Engineer")
                                        .suggestedValue("Lead Architect")
                                        .sourceSnippet("Role: Lead Architect at Google")
                                        .rationale("Recent promotion noted in document")
                                        .build()
                        ))
                        .conflictResolutions(List.of(
                                AiSemanticReviewResult.AiConflictResolution.builder()
                                        .field("occupation")
                                        .context("CANDIDATE")
                                        .recommendedValue("Lead Architect")
                                        .rationale("Recent promotion noted in document")
                                        .build()
                        ))
                        .build());
            }

            @Override
            public boolean isAvailable() { return true; }
            @Override
            public String getProviderName() { return "MockAI"; }
            @Override
            public String getModelName() { return "mock-model"; }
        };

        BiodataParserImplementation parser = new BiodataParserImplementation(mockProvider, routingPolicy, converter);

        String rawText = """
                Name: Satwik Kotte
                DOB: 15-08-1995
                Job: Software Engineer
                Caste: Brahmin
                Education: B.Tech
                Role: Lead Architect at Google
                """;

        ParseResponse response = parser.parseAndValidate(rawText, true);

        assertEquals(FieldConfidence.CONFLICT, response.getConfidenceScores().get("occupation"));
        assertFalse(response.getConflicts().isEmpty(), "Contradictory values must generate a conflict record");

        ConflictRecord conflict = response.getConflicts().stream()
                .filter(cr -> "occupation".equals(cr.getKey().field().getPropertyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(conflict);
        assertEquals("Lead Architect", conflict.getRecommendedValue(), "AI recommendation must be attached to conflict");
        assertEquals("Recent promotion noted in document", conflict.getRecommendationRationale());
    }

    @Test
    void testDetMissingAndAiSupportedAcceptedViaReconciliation() {
        AiExtractionProvider mockProvider = new AiExtractionProvider() {
            @Override
            public Optional<AiSemanticReviewResult> reviewAndComplete(DeterministicExtractionSummary summary) {
                return Optional.of(AiSemanticReviewResult.builder()
                        .completions(List.of(
                                AiSemanticReviewResult.AiCompletion.builder()
                                        .field("salary")
                                        .context("CANDIDATE")
                                        .value("25 LPA")
                                        .confidence("HIGH")
                                        .sourceSnippet("Package: 25 LPA")
                                        .rationale("Extracted from unstructured text")
                                        .build()
                        ))
                        .build());
            }

            @Override
            public boolean isAvailable() { return true; }
            @Override
            public String getProviderName() { return "MockAI"; }
            @Override
            public String getModelName() { return "mock-model"; }
        };

        BiodataParserImplementation parser = new BiodataParserImplementation(mockProvider, routingPolicy, converter);

        String rawText = """
                Name: Satwik Kotte
                DOB: 15-08-1995
                Job: Software Engineer
                Caste: Brahmin
                Education: B.Tech
                Package: 25 LPA
                """;

        ParseResponse response = parser.parseAndValidate(rawText, true);

        assertEquals("25 LPA", response.getProfile().getSalary(), "Missing field should be populated by AI completion");
        assertEquals(FieldConfidence.HIGH, response.getConfidenceScores().get("salary"));
    }

    @Test
    void testAiFailurePreservesDeterministicBaselineWithWarning() {
        AiExtractionProvider failingProvider = new AiExtractionProvider() {
            @Override
            public Optional<AiSemanticReviewResult> reviewAndComplete(DeterministicExtractionSummary summary) {
                // Simulate HTTP 429, timeout, or network error
                return Optional.empty();
            }

            @Override
            public boolean isAvailable() { return true; }
            @Override
            public String getProviderName() { return "FailingAI"; }
            @Override
            public String getModelName() { return "failing-model"; }
        };

        BiodataParserImplementation parser = new BiodataParserImplementation(failingProvider, routingPolicy, converter);

        String rawText = """
                Name: Satwik Kotte
                DOB: 15-08-1995
                Job: Software Engineer
                Caste: Brahmin
                Education: B.Tech
                """;

        ParseResponse response = parser.parseAndValidate(rawText, true);

        // Core deterministic fields are 100% intact
        assertEquals("Satwik Kotte", response.getProfile().getFullName());
        assertEquals("Software Engineer", response.getProfile().getOccupation());
        assertEquals(FieldConfidence.HIGH, response.getConfidenceScores().get("fullName"));

        // Warning was registered gracefully
        assertNotNull(response.getAiMetadata());
        assertTrue(response.getAiMetadata().isInvoked());
        assertNotNull(response.getAiMetadata().getErrorMessage());

        boolean hasAiWarning = response.getWarnings().stream()
                .anyMatch(w -> w.getCategory() == WarningCategory.AI_SERVICE_UNAVAILABLE);
        assertTrue(hasAiWarning, "ParseResponse must contain AI_SERVICE_UNAVAILABLE warning when AI fails");
    }
}

