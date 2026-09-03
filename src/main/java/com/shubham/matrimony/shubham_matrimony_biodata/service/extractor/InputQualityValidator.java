package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Post-parse analysis that classifies the engine's output into a
 * {@link ParseStatus} and generates categorized {@link ParseWarning}s.
 *
 * <p>
 * This class sits <em>after</em> the parser engine, not before it.
 * It does not modify the profile or re-parse anything — it only
 * inspects what the engine produced and decides:
 * <ol>
 * <li>Was the parse successful?</li>
 * <li>Are there warnings the operator should see?</li>
 * </ol>
 *
 * <p>
 * Design principles:
 * <ul>
 * <li>MISSING means "parser didn't find this" — <em>not</em> "input is
 * invalid."</li>
 * <li>The system is designed for human completion. Missing fields are
 * expected.</li>
 * <li>Never silently discard information.</li>
 * </ul>
 */
public class InputQualityValidator {

    /**
     * Number of HIGH fields below which we emit an informational
     * {@link WarningCategory#MISSING_EXPECTED_INFORMATION} warning.
     * This is NOT a rejection threshold — even 1 field is a valid parse.
     */
    private static final int LOW_FIELD_THRESHOLD = 5;

    /**
     * Classifies the overall parse outcome.
     *
     * <ul>
     * <li>0 HIGH fields → {@link ParseStatus#REJECTED_INPUT}</li>
     * <li>≥1 HIGH fields + unparsedLines non-empty →
     * {@link ParseStatus#SUCCESS_WITH_WARNINGS}</li>
     * <li>≥1 HIGH fields + unparsedLines empty → {@link ParseStatus#SUCCESS}</li>
     * </ul>
     *
     * @param confidenceScores the field confidence map produced by the engine
     * @param unparsedLines    lines the parser could not map
     * @return the classification status
     */
    public ParseStatus classify(Map<String, FieldConfidence> confidenceScores,
            List<String> unparsedLines) {

        long highCount = countHighFields(confidenceScores);

        if (highCount == 0) {
            return ParseStatus.REJECTED_INPUT;
        }
        if (unparsedLines != null && !unparsedLines.isEmpty()) {
            return ParseStatus.SUCCESS_WITH_WARNINGS;
        }
        return ParseStatus.SUCCESS;
    }

    /**
     * Generates human-readable warnings based on the classification.
     *
     * <ul>
     * <li>{@link ParseStatus#REJECTED_INPUT} →
     * {@link WarningCategory#LOW_INFORMATION_INPUT}</li>
     * <li>{@link ParseStatus#SUCCESS_WITH_WARNINGS}:
     * <ul>
     * <li>{@link WarningCategory#UNRECOGNIZED_TEXT} with unparsed lines as
     * details</li>
     * <li>{@link WarningCategory#MISSING_EXPECTED_INFORMATION} if &lt;
     * {@value #LOW_FIELD_THRESHOLD}
     * fields recognized (informational only)</li>
     * </ul>
     * </li>
     * <li>{@link ParseStatus#SUCCESS} → empty list</li>
     * </ul>
     *
     * @param status           the classification from {@link #classify}
     * @param confidenceScores the field confidence map
     * @param unparsedLines    lines the parser could not map
     * @return list of warnings (may be empty, never null)
     */
    public List<ParseWarning> generateWarnings(ParseStatus status,
            Map<String, FieldConfidence> confidenceScores,
            List<String> unparsedLines) {

        List<ParseWarning> warnings = new ArrayList<>();

        if (status == ParseStatus.REJECTED_INPUT) {
            warnings.add(ParseWarning.builder()
                    .category(WarningCategory.LOW_INFORMATION_INPUT)
                    .message("The supplied text does not appear to contain a supported biodata format. "
                            + "No recognized fields were found.")
                    .build());
            return warnings;
        }

        if (status == ParseStatus.SUCCESS) {
            return warnings; // empty — no issues
        }

        // SUCCESS_WITH_WARNINGS
        if (unparsedLines != null && !unparsedLines.isEmpty()) {
            warnings.add(ParseWarning.builder()
                    .category(WarningCategory.UNRECOGNIZED_TEXT)
                    .message(unparsedLines.size()
                            + (unparsedLines.size() == 1
                                    ? " line could not be mapped to standard biodata fields."
                                    : " lines could not be mapped to standard biodata fields."))
                    .details(new ArrayList<>(unparsedLines))
                    .build());
        }

        long highCount = countHighFields(confidenceScores);
        long totalFields = confidenceScores.size();
        if (highCount < LOW_FIELD_THRESHOLD && totalFields > 0) {
            warnings.add(ParseWarning.builder()
                    .category(WarningCategory.MISSING_EXPECTED_INFORMATION)
                    .message("Only " + highCount + " of " + totalFields
                            + " fields were recognized. Please verify the result.")
                    .build());
        }

        return warnings;
    }

    private long countHighFields(Map<String, FieldConfidence> confidenceScores) {
        if (confidenceScores == null || confidenceScores.isEmpty()) {
            return 0;
        }
        return confidenceScores.values().stream()
                .filter(c -> c == FieldConfidence.HIGH)
                .count();
    }
}
