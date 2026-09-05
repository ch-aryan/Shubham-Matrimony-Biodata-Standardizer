package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.EvidenceKey;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiRoutingPolicyTest {

    private GeminiConfigProperties config;
    private GeminiRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        config = new GeminiConfigProperties();
        config.getApi().setEnabled(true);
        config.getApi().setKey("test-api-key");
        config.getRouting().setMinFieldsThreshold(5);
        config.getRouting().setAlwaysCallIfConflict(true);
        config.getRouting().setAlwaysCallIfUnparsed(true);
        policy = new GeminiRoutingPolicy(config);
    }

    @Test
    void testForceAiInvokesWhenProviderAvailable() {
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder().build();
        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, true);
        assertTrue(decision.shouldInvoke());
        assertEquals("FORCED_BY_REQUEST", decision.reason());
    }

    @Test
    void testForceAiFailsWhenProviderUnavailable() {
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder().build();
        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, false, true);
        assertFalse(decision.shouldInvoke());
        assertEquals("FORCE_AI_REQUESTED_BUT_PROVIDER_UNAVAILABLE", decision.reason());
    }

    @Test
    void testDisabledInConfigSkipsAi() {
        config.getApi().setEnabled(false);
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder().build();
        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertFalse(decision.shouldInvoke());
        assertEquals("AI_DISABLED_IN_CONFIG", decision.reason());
    }

    @Test
    void testProviderUnavailableSkipsAi() {
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder().build();
        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, false, false);
        assertFalse(decision.shouldInvoke());
        assertEquals("PROVIDER_UNAVAILABLE", decision.reason());
    }

    @Test
    void testActiveConflictsTriggersAi() {
        ConflictRecord cr = ConflictRecord.builder()
                .key(new EvidenceKey(ExtractionContext.CANDIDATE, BiodataField.OCCUPATION))
                .competingValues(List.of("Dev", "Manager"))
                .build();

        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .conflicts(List.of(cr))
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertTrue(decision.shouldInvoke());
        assertTrue(decision.reason().contains("CONFLICTS_DETECTED"));
    }

    @Test
    void testMeaningfulUnparsedLinesTriggersAi() {
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .unparsedLines(List.of("Brother: Rohil Thota works at Google"))
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertTrue(decision.shouldInvoke());
        assertTrue(decision.reason().contains("UNPARSED_LINES_PRESENT"));
    }

    @Test
    void testMissingCriticalFieldTriggersAi() {
        Map<String, FieldConfidence> scores = new HashMap<>();
        scores.put("dateOfBirth", FieldConfidence.HIGH);
        scores.put("occupation", FieldConfidence.HIGH);
        scores.put("caste", FieldConfidence.HIGH);
        scores.put("qualification", FieldConfidence.HIGH);
        // fullName is missing

        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .confidenceScores(scores)
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertTrue(decision.shouldInvoke());
        assertTrue(decision.reason().contains("CRITICAL_FIELD_MISSING (fullName)"));
    }

    @Test
    void testLowCoverageTriggersAi() {
        Map<String, FieldConfidence> scores = new HashMap<>();
        scores.put("fullName", FieldConfidence.HIGH);
        scores.put("dateOfBirth", FieldConfidence.HIGH);
        scores.put("occupation", FieldConfidence.HIGH);
        scores.put("caste", FieldConfidence.HIGH);
        scores.put("qualification", FieldConfidence.HIGH);
        // populated count is 5, but threshold set to 8

        config.getRouting().setMinFieldsThreshold(8);
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .confidenceScores(scores)
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertTrue(decision.shouldInvoke());
        assertTrue(decision.reason().contains("LOW_COVERAGE"));
    }

    @Test
    void testSolidBaselineWithZeroGapsSkipsAi() {
        Map<String, FieldConfidence> scores = new HashMap<>();
        scores.put("fullName", FieldConfidence.HIGH);
        scores.put("dateOfBirth", FieldConfidence.HIGH);
        scores.put("occupation", FieldConfidence.HIGH);
        scores.put("caste", FieldConfidence.HIGH);
        scores.put("qualification", FieldConfidence.HIGH);
        scores.put("height", FieldConfidence.HIGH);
        scores.put("fatherName", FieldConfidence.HIGH);
        scores.put("motherName", FieldConfidence.HIGH);

        config.getRouting().setMinFieldsThreshold(5);
        ProfileBiodata profile = new ProfileBiodata();
        profile.setFullName("Satwik");
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .confidenceScores(scores)
                .conflicts(List.of())
                .unparsedLines(List.of())
                .profile(profile)
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = policy.evaluate(summary, true, false);
        assertFalse(decision.shouldInvoke());
        assertEquals("HIGH_CONFIDENCE_BASELINE", decision.reason());
    }
}
