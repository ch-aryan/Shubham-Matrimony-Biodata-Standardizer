package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.AdditionalInfoExtractor;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ConfidenceScorer;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.EducationExtractor;
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
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.MergeResult;
import com.shubham.matrimony.shubham_matrimony_biodata.service.extractor.ExtractionMerger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the biodata parsing pipeline by delegating each responsibility
 * to a focused single-purpose extractor.
 *
 * <p>
 * Each stage is an independent, testable class — see the
 * {@code service/extractor/} package for details.
 *
 * <h3>Pipeline per line</h3>
 * <ol>
 * <li>{@link InputNormalizer} — splits raw text, flattens pipe-separated
 * rows.</li>
 * <li>{@link ScopeTracker} early — resets array state, counts braces, detects
 * family block.</li>
 * <li>Ignorable / conversational — {@link BiodataParserUtils#isIgnorableLine},
 * {@link BiodataParserUtils#isConversationalNote}.</li>
 * <li>{@link EducationExtractor} — multi-line JSON array item
 * continuation.</li>
 * <li>{@link ScopeTracker} late — family sub-section transitions, sibling close
 * flush.</li>
 * <li>{@link PersonalExtractor} — standalone height / born (early).</li>
 * <li>{@link EducationExtractor} — standalone degree-prefix.</li>
 * <li>{@link OccupationExtractor} — standalone "role @ company".</li>
 * <li>{@link FamilyExtractor} — compound "Father X – job" lines.</li>
 * <li>{@link PersonalExtractor} — unlabeled candidate name (after other
 * heuristics).</li>
 * <li>{@link FamilyExtractor} — standalone father-job / homemaker.</li>
 * <li>{@link EducationExtractor} — array-open bracket detection.</li>
 * <li>Label scan — {@link BiodataParserUtils#parseTextSegments}.</li>
 * <li>Segment dispatch — OCCUPATION → {@link OccupationExtractor};
 * horoscope fields → {@link HoroscopeExtractor};
 * family fields → {@link FamilyExtractor};
 * rest → {@link PropertyExtractor}.</li>
 * </ol>
 *
 * <h3>Post-processing</h3>
 * {@link ConfidenceScorer} flushes the last sibling, merges the candidate name,
 * and computes the {@link FieldConfidence} map.
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
    private final InputQualityValidator validator = new InputQualityValidator();
    private final ExtractionMerger merger = new ExtractionMerger();

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
     *
     * @param rawText raw unformatted biodata text (WhatsApp, Telugu, JSON-like,
     *                etc.)
     * @return {@link ExtractionResultDTO} with profile, confidence scores, unparsed
     *         lines, and warnings
     */
    @Override
    public ExtractionResultDTO parseBiodata(String rawText) {
        Map<String, FieldConfidence> confidenceScores = new HashMap<>();

        // Fast-path: blank or null input
        if (rawText == null || rawText.isBlank()) {
            scorer.populateMissingConfidence(confidenceScores);
            return buildResult(new ParseContext(), confidenceScores, null);
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

        // ── Stage 8: post-processing ──────────────────────────────────────────
        scorer.finalizeProfile(ctx, confidenceScores);
        MergeResult mergeResult = merger.merge(ctx.evidenceList, ctx.profile);
        for (ConflictRecord cr : mergeResult.getConflicts()) {
            if (cr.getKey() != null && cr.getKey().field() != null) {
                confidenceScores.put(cr.getKey().field().getPropertyName(), FieldConfidence.CONFLICT);
            }
        }
        return buildResult(ctx, confidenceScores, mergeResult);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Full parse with post-parse validation and warning generation.
     *
     * <p>
     * Delegates to {@link #parseBiodata(String)} (engine is untouched), then
     * passes the result to {@link InputQualityValidator} for classification.
     *
     * @param rawText raw unformatted biodata text
     * @return {@link ParseResponse} with status, profile, confidence scores,
     *         categorized warnings, and all unparsed lines (no truncation)
     */
    @Override
    public ParseResponse parseAndValidate(String rawText) {
        ExtractionResultDTO result = parseBiodata(rawText);

        ParseStatus status = validator.classify(
                result.getConfidenceScores(),
                result.getUnparsedLines());

        List<ParseWarning> warnings = validator.generateWarnings(
                status,
                result.getConfidenceScores(),
                result.getUnparsedLines());

        return ParseResponse.builder()
                .status(status)
                .profile(status == ParseStatus.REJECTED_INPUT ? null : result.getProfile())
                .confidenceScores(result.getConfidenceScores())
                .warnings(warnings)
                .unparsedLines(result.getUnparsedLines())
                .build();
    }

    private ExtractionResultDTO buildResult(ParseContext ctx,
            Map<String, FieldConfidence> confidenceScores,
            MergeResult mergeResult) {
        return ExtractionResultDTO.builder()
                .profile(ctx.profile)
                .confidenceScores(confidenceScores)
                .unparsedLines(ctx.unparsedLines)
                .warnings(ctx.warnings)
                .conflicts(mergeResult != null ? mergeResult.getConflicts() : List.of())
                .evidenceTrail(ctx.evidenceList)
                .build();
    }
}