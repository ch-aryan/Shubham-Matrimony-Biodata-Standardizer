package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether a parsing request requires AI semantic review or whether
 * the deterministic baseline is sufficient.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiRoutingPolicy {

    private final GeminiConfigProperties config;

    private static final Set<String> CORE_CANONICAL_FIELDS = Set.of(
            "fullName",
            "dateOfBirth",
            "occupation",
            "caste",
            "qualification"
    );

    public record RoutingDecision(boolean shouldInvoke, String reason) {}

    /**
     * Evaluates the routing criteria against deterministic findings.
     *
     * @param summary findings from deterministic parser
     * @param isProviderAvailable whether the provider can be reached
     * @param forceAi explicit override flag from request
     * @return decision with invocation flag and human-readable reason
     */
    public RoutingDecision evaluate(DeterministicExtractionSummary summary, boolean isProviderAvailable, Boolean forceAi) {
        if (Boolean.TRUE.equals(forceAi)) {
            if (!isProviderAvailable) {
                return new RoutingDecision(false, "FORCE_AI_REQUESTED_BUT_PROVIDER_UNAVAILABLE");
            }
            return new RoutingDecision(true, "FORCED_BY_REQUEST");
        }

        if (!config.getApi().isEnabled()) {
            return new RoutingDecision(false, "AI_DISABLED_IN_CONFIG");
        }

        if (!isProviderAvailable) {
            return new RoutingDecision(false, "PROVIDER_UNAVAILABLE");
        }

        if (summary == null) {
            return new RoutingDecision(false, "NO_SUMMARY");
        }

        // 1. Active conflicts
        if (config.getRouting().isAlwaysCallIfConflict() && summary.getConflicts() != null && !summary.getConflicts().isEmpty()) {
            return new RoutingDecision(true, "CONFLICTS_DETECTED (" + summary.getConflicts().size() + " conflicts)");
        }

        // 2. Unparsed lines
        if (config.getRouting().isAlwaysCallIfUnparsed() && summary.getUnparsedLines() != null && !summary.getUnparsedLines().isEmpty()) {
            long meaningfulLines = summary.getUnparsedLines().stream()
                    .filter(l -> l != null && !l.trim().isEmpty() && !"{".equals(l.trim()) && !"}".equals(l.trim()))
                    .count();
            if (meaningfulLines > 0) {
                return new RoutingDecision(true, "UNPARSED_LINES_PRESENT (" + meaningfulLines + " lines)");
            }
        }

        // 3. Missing core canonical fields
        Map<String, FieldConfidence> scores = summary.getConfidenceScores();
        if (scores != null) {
            for (String coreField : CORE_CANONICAL_FIELDS) {
                FieldConfidence conf = scores.get(coreField);
                if (conf == null || conf == FieldConfidence.MISSING) {
                    return new RoutingDecision(true, "CRITICAL_FIELD_MISSING (" + coreField + ")");
                }
            }
        }

        // 4. Low coverage threshold check
        int minThreshold = config.getRouting().getMinFieldsThreshold();
        long populatedCount = 0;
        if (scores != null) {
            populatedCount = scores.values().stream()
                    .filter(c -> c == FieldConfidence.HIGH || c == FieldConfidence.MEDIUM || c == FieldConfidence.LOW)
                    .count();
        }
        if (populatedCount < minThreshold) {
            return new RoutingDecision(true, "LOW_COVERAGE (" + populatedCount + " < " + minThreshold + " fields)");
        }

        // All core fields present, coverage high, 0 conflicts, 0 unparsed lines
        return new RoutingDecision(false, "HIGH_CONFIDENCE_BASELINE");
    }
}

