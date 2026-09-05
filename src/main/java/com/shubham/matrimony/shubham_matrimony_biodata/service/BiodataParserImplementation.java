package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.AiReviewMetadata;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.MergeResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.AiExtractionProvider;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.AiExtractionResultConverter;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.AiSemanticReviewResult;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.DeterministicExtractionSummary;
import com.shubham.matrimony.shubham_matrimony_biodata.service.ai.GeminiRoutingPolicy;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.AdditionalInfoExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ConfidenceScorer;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ProfileFinalizer;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.EducationExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ExtractionMerger;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.FamilyExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.HoroscopeExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.InputNormalizer;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.InputQualityValidator;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.OccupationExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ParseContext;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.PersonalExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.PropertyExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ScopeTracker;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils.ParsedSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the biodata parsing pipeline:
 * Stage 1-7: Line-by-line deterministic extraction
 * Stage 8: Deterministic post-processing & baseline merge
 * Stage 9: AI Semantic Review Layer (Gemini)
 * Stage 10: Final Reconciliation via ExtractionMerger
 */
@Service
public class BiodataParserImplementation implements BiodataServiceParser {

    // ── Extractors wired as plain Java objects (no Spring context needed) ─────
    private final FamilyExtractor familyExtractor = new FamilyExtractor();
    private final HoroscopeExtractor horoscopeExtractor = new HoroscopeExtractor();
    private final InputNormalizer normalizer = new InputNormalizer();
    private final ScopeTracker scopeTracker = new ScopeTracker(familyExtractor);
    private final PersonalExtractor personalExtractor = new PersonalExtractor();
    private final EducationExtractor educationExtractor = new EducationExtractor();
    private final OccupationExtractor occupationExtractor = new OccupationExtractor();
    private final PropertyExtractor propertyExtractor = new PropertyExtractor();
    private final AdditionalInfoExtractor additionalInfoExtractor = new AdditionalInfoExtractor();
    private final ConfidenceScorer scorer = new ConfidenceScorer(familyExtractor);
    private final ProfileFinalizer profileFinalizer = new ProfileFinalizer(familyExtractor);
    private final InputQualityValidator validator = new InputQualityValidator();
    private final ExtractionMerger merger = new ExtractionMerger();

    // ── AI Semantic Layer Dependencies (Optional / Spring Injected) ───────────
    @Autowired(required = false)
    private AiExtractionProvider aiProvider;

    @Autowired(required = false)
    private GeminiRoutingPolicy routingPolicy;

    @Autowired(required = false)
    private AiExtractionResultConverter aiConverter;

    public BiodataParserImplementation() {
    }

    public BiodataParserImplementation(AiExtractionProvider aiProvider,
            GeminiRoutingPolicy routingPolicy,
            AiExtractionResultConverter aiConverter) {
        this.aiProvider = aiProvider;
        this.routingPolicy = routingPolicy;
        this.aiConverter = aiConverter;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Convenience entry point — returns only the populated {@link ProfileBiodata}.
     */
    @Override
    public ProfileBiodata parse(String rawText) {
        return parseBiodata(rawText).getProfile();
    }

    /**
     * Full parse entry point.
     */
    @Override
    public ExtractionResultDTO parseBiodata(String rawText) {
        return parseBiodata(rawText, null);
    }

    @Override
    public ExtractionResultDTO parseBiodata(String rawText, Boolean forceAi) {
        Map<String, FieldConfidence> confidenceScores = new HashMap<>();

        // Fast-path: blank or null input
        if (rawText == null || rawText.isBlank()) {
            scorer.populateMissingConfidence(confidenceScores);
            profileFinalizer.populateMissingConfidence(confidenceScores);
            return buildResult(new ParseContext(), confidenceScores, null, null);
        }

        ParseContext ctx = new ParseContext();
        List<String> lines = normalizer.normalize(rawText);

        for (String rawLine : lines) {
            String sanitized = BiodataParserUtils.sanitizeLine(rawLine);
            String lowerLine = sanitized.toLowerCase().trim();

            // ── Stage 1: early scope (brace depth + family block entry/exit) ──
            scopeTracker.updateEarly(rawLine, lowerLine, ctx);

            // ── Stage 2: skip non-data lines ──────────────────────────────────
            if (BiodataParserUtils.isIgnorableLine(sanitized))
                continue;
            if (additionalInfoExtractor.tryExtract(sanitized, ctx))
                continue;
            if (BiodataParserUtils.isConversationalNote(lowerLine)) {
                ctx.unparsedLines.add(sanitized);
                continue;
            }

            // ── Stage 3: multi-line JSON array item continuation ──────────────
            if (educationExtractor.tryExtractArrayItem(sanitized, ctx))
                continue;

            // ── Stage 4: late scope (family sub-section + sibling close flush) ─
            scopeTracker.updateLate(sanitized, lowerLine, ctx);

            // ── Stage 5: standalone heuristics (order matches original engine) ─
            if (personalExtractor.tryExtractEarlyHeuristics(sanitized, lowerLine, ctx))
                continue;
            if (educationExtractor.tryExtractStandalone(sanitized, ctx))
                continue;
            if (occupationExtractor.tryExtractStandalone(sanitized, lowerLine, ctx))
                continue;
            if (familyExtractor.tryExtractCompoundParentLine(sanitized, lowerLine, ctx))
                continue;
            if (personalExtractor.tryExtractUnlabeledName(sanitized, lowerLine, ctx))
                continue;
            if (familyExtractor.tryExtractStandaloneFatherJob(sanitized, ctx))
                continue;
            if (familyExtractor.tryExtractStandaloneMotherOccupation(sanitized, ctx))
                continue;
            if (educationExtractor.tryDetectArrayOpen(sanitized, ctx))
                continue;

            // ── Stage 6: label-based segment extraction ───────────────────────
            List<ParsedSegment> segments = BiodataParserUtils.parseTextSegments(sanitized);
            if (segments.isEmpty()) {
                if (propertyExtractor.tryCaptureCustomAttribute(sanitized, ctx)) {
                    continue;
                }
                if (!BiodataParserUtils.isIgnorableLine(sanitized)) {
                    ctx.unparsedLines.add(sanitized);
                }
                continue;
            }

            // ── Stage 7: segment dispatch ─────────────────────────────────────
            for (ParsedSegment segment : segments) {
                BiodataField field = segment.getField();
                String value = segment.getValue();

                // OCCUPATION is handled before family routing (preserves original order)
                if (field == BiodataField.OCCUPATION) {
                    occupationExtractor.applyOccupationSegment(value, ctx);
                    continue;
                }

                // Horoscope fields: RASHI, NAKSHATRAM, GOTHRAM
                if (horoscopeExtractor.tryApply(field, value, ctx)) {
                    continue;
                }

                // Family fields: FATHER_NAME, MOTHER_NAME, SIBLINGS, NATIVE_PLACE, etc.
                if (familyExtractor.tryApply(field, value, ctx))
                    continue;

                // Candidate-level: SURNAME, FULL_NAME, SIBLINGS, and generic apply
                propertyExtractor.apply(field, value, ctx);
            }
        }

        // ── Stage 8: deterministic post-processing & baseline merge ───────────
        scorer.finalizeProfile(ctx, confidenceScores);
        profileFinalizer.finalizeProfile(ctx);
        MergeResult baselineMerge = merger.merge(ctx.evidenceList, ctx.profile);
        if (baselineMerge != null && baselineMerge.getConfidenceScores() != null) {
            confidenceScores.putAll(baselineMerge.getConfidenceScores());
        }

        // ── Stage 9: AI Semantic Review Layer ─────────────────────────────────
        List<AiSemanticReviewResult.AiConflictResolution> aiConflictResolutions = new ArrayList<>();
        AiReviewMetadata aiMetadata = performAiReview(ctx, rawText, baselineMerge, confidenceScores, forceAi,
                aiConflictResolutions);

        // ── Stage 10: Final Reconciliation via ExtractionMerger ───────────────
        MergeResult finalMerge;
        if (aiMetadata != null && aiMetadata.isInvoked() && aiMetadata.getErrorMessage() == null) {
            finalMerge = merger.merge(ctx.evidenceList, ctx.profile, aiConflictResolutions);
        } else {
            finalMerge = baselineMerge;
        }

        if (finalMerge != null && finalMerge.getConfidenceScores() != null) {
            confidenceScores.putAll(finalMerge.getConfidenceScores());
        }
        if (finalMerge != null && finalMerge.getConflicts() != null) {
            for (ConflictRecord cr : finalMerge.getConflicts()) {
                if (cr.getKey() != null && cr.getKey().field() != null) {
                    confidenceScores.put(cr.getKey().field().getPropertyName(), FieldConfidence.CONFLICT);
                }
            }
        }

        return buildResult(ctx, confidenceScores, finalMerge, aiMetadata);
    }

    private AiReviewMetadata performAiReview(ParseContext ctx,
            String rawText,
            MergeResult baselineMerge,
            Map<String, FieldConfidence> confidenceScores,
            Boolean forceAi,
            List<AiSemanticReviewResult.AiConflictResolution> outConflictResolutions) {
        if (aiProvider == null || routingPolicy == null) {
            return AiReviewMetadata.builder()
                    .invoked(false)
                    .routingReason("AI_LAYER_NOT_CONFIGURED")
                    .build();
        }

        List<String> missingFields = findMissingCanonicalFields(confidenceScores);
        DeterministicExtractionSummary summary = DeterministicExtractionSummary.builder()
                .rawText(rawText)
                .profile(baselineMerge != null && baselineMerge.getProfile() != null ? baselineMerge.getProfile()
                        : ctx.profile)
                .confidenceScores(new HashMap<>(confidenceScores))
                .conflicts(baselineMerge != null && baselineMerge.getConflicts() != null
                        ? new ArrayList<>(baselineMerge.getConflicts())
                        : new ArrayList<>())
                .evidenceTrail(new ArrayList<>(ctx.evidenceList))
                .unparsedLines(new ArrayList<>(ctx.unparsedLines))
                .missingCanonicalFields(missingFields)
                .build();

        GeminiRoutingPolicy.RoutingDecision decision = routingPolicy.evaluate(summary, aiProvider.isAvailable(),
                forceAi);
        if (!decision.shouldInvoke()) {
            return AiReviewMetadata.builder()
                    .invoked(false)
                    .routingReason(decision.reason())
                    .build();
        }

        long startTime = System.currentTimeMillis();
        Optional<AiSemanticReviewResult> reviewOpt = aiProvider.reviewAndComplete(summary);
        long latency = System.currentTimeMillis() - startTime;

        if (reviewOpt.isEmpty()) {
            return AiReviewMetadata.builder()
                    .invoked(true)
                    .provider(aiProvider.getProviderName())
                    .model(aiProvider.getModelName())
                    .latencyMs(latency)
                    .routingReason(decision.reason())
                    .errorMessage("AI review did not return a valid response")
                    .build();
        }

        AiSemanticReviewResult aiResult = reviewOpt.get();
        if (aiResult.getConflictResolutions() != null) {
            outConflictResolutions.addAll(aiResult.getConflictResolutions());
        }

        if (aiConverter != null) {
            List<ExtractionResult> aiEvidence = aiConverter.convertToEvidence(aiResult);
            ctx.evidenceList.addAll(aiEvidence);
        }

        List<String> completed = aiResult.getCompletions() != null
                ? aiResult.getCompletions().stream().map(AiSemanticReviewResult.AiCompletion::getField).distinct()
                        .toList()
                : List.of();
        List<String> corrected = aiResult.getCorrections() != null
                ? aiResult.getCorrections().stream().map(AiSemanticReviewResult.AiCorrection::getField).distinct()
                        .toList()
                : List.of();
        List<String> reviewed = aiResult.getConflictResolutions() != null
                ? aiResult.getConflictResolutions().stream().map(AiSemanticReviewResult.AiConflictResolution::getField)
                        .distinct().toList()
                : List.of();

        return AiReviewMetadata.builder()
                .invoked(true)
                .provider(aiProvider.getProviderName())
                .model(aiProvider.getModelName())
                .latencyMs(latency)
                .routingReason(decision.reason())
                .fieldsCompleted(completed)
                .fieldsCorrected(corrected)
                .conflictsReviewed(reviewed)
                .build();
    }

    private List<String> findMissingCanonicalFields(Map<String, FieldConfidence> scores) {
        List<String> missing = new ArrayList<>();
        for (BiodataField field : BiodataField.values()) {
            if (field.isCanonical()) {
                FieldConfidence conf = scores.get(field.getPropertyName());
                if (conf == null || conf == FieldConfidence.MISSING) {
                    missing.add(field.getPropertyName());
                }
            }
        }
        return missing;
    }

    @Override
    public ParseResponse parseAndValidate(String rawText) {
        return parseAndValidate(rawText, null);
    }

    @Override
    public ParseResponse parseAndValidate(String rawText, Boolean forceAi) {
        ExtractionResultDTO result = parseBiodata(rawText, forceAi);

        ParseStatus status = validator.classify(
                result.getConfidenceScores(),
                result.getUnparsedLines());

        List<ParseWarning> warnings = validator.generateWarnings(
                status,
                result.getConfidenceScores(),
                result.getUnparsedLines());

        if (result.getAiMetadata() != null && result.getAiMetadata().getErrorMessage() != null) {
            warnings.add(ParseWarning.builder()
                    .category(WarningCategory.AI_SERVICE_UNAVAILABLE)
                    .message("AI Review unavailable: " + result.getAiMetadata().getErrorMessage()
                            + ". Deterministic results preserved.")
                    .build());
        }

        return ParseResponse.builder()
                .status(status)
                .profile(status == ParseStatus.REJECTED_INPUT ? null : result.getProfile())
                .confidenceScores(result.getConfidenceScores())
                .conflicts(result.getConflicts())
                .evidenceTrail(result.getEvidenceTrail())
                .warnings(warnings)
                .unparsedLines(result.getUnparsedLines())
                .aiMetadata(result.getAiMetadata())
                .build();
    }

    private ExtractionResultDTO buildResult(ParseContext ctx,
            Map<String, FieldConfidence> confidenceScores,
            MergeResult mergeResult,
            AiReviewMetadata aiMetadata) {
        ProfileBiodata resolvedProfile = (mergeResult != null && mergeResult.getProfile() != null)
                ? mergeResult.getProfile()
                : ctx.profile;
        return ExtractionResultDTO.builder()
                .profile(resolvedProfile)
                .confidenceScores(confidenceScores)
                .unparsedLines(ctx.unparsedLines)
                .warnings(ctx.warnings)
                .conflicts(mergeResult != null ? mergeResult.getConflicts() : List.of())
                .evidenceTrail(ctx.evidenceList)
                .aiMetadata(aiMetadata)
                .build();
    }
}