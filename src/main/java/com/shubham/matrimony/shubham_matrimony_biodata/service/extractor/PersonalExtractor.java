package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils;

/**
 * Extracts personal candidate fields that appear <em>without</em> an explicit
 * label.
 *
 * <p>
 * Two phases are exposed because the original engine's order matters:
 * <ul>
 * <li>{@link #tryExtractEarlyHeuristics} — height and "born" suffix.
 * Must run <em>before</em> education and occupation heuristics.</li>
 * <li>{@link #tryExtractUnlabeledName} — unlabeled name at the top of the
 * document.
 * Must run <em>after</em> education and occupation heuristics (so "B.Tech JNTU"
 * is
 * not mistakenly captured as a name).</li>
 * </ul>
 */
public class PersonalExtractor {

    // ── Phase A — run BEFORE education / occupation heuristics ───────────────

    /**
     * Attempts to detect a standalone height or suffix-born place-of-birth.
     *
     * <p>
     * <b>Height examples:</b> {@code 6ft}, {@code 5'9"}, {@code 5.8},
     * {@code 5ft 10in}
     * <p>
     * <b>Born-suffix examples:</b> {@code Nizamabad born},
     * {@code at Hyderabad born}
     *
     * @return {@code true} if a field was consumed; the caller should
     *         {@code continue}.
     */
    public boolean tryExtractEarlyHeuristics(String sanitized, String lowerLine, ParseContext ctx) {
        // Standalone height (e.g. "6ft", "5'9", "5.8", "5ft 10in")
        if (ctx.profile.getHeight() == null
                && !sanitized.contains(":")
                && !sanitized.contains(",")
                && sanitized.matches(
                        "(?i)^[4-7]\\s*('|\"|ft|feet|\\.)(\\s*\\d{1,2}(\"|in|inches)?)?$"
                                + "|^[4-7]\\s*(ft|feet)$")) {
            ctx.profile.setHeight(sanitized);
            ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                    .field(BiodataField.HEIGHT)
                    .value(sanitized)
                    .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                    .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.MEDIUM)
                    .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.HEURISTIC)
                    .sourceText(sanitized)
                    .build());
            return true;
        }

        // Suffix born pattern (e.g. "Nizamabad born", "Hyderabad born")
        if (!sanitized.contains(":")
                && (lowerLine.endsWith(" born") || lowerLine.endsWith(" born."))) {
            String place = sanitized.substring(0, lowerLine.lastIndexOf(" born")).trim();
            if (place.toLowerCase().startsWith("at ") || place.toLowerCase().startsWith("in ")) {
                place = place.substring(3).trim();
            }
            if (ctx.profile.getPlaceOfBirth() == null || ctx.profile.getPlaceOfBirth().isBlank()) {
                ctx.profile.setPlaceOfBirth(place);
                ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                        .field(BiodataField.PLACE_OF_BIRTH)
                        .value(place)
                        .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                        .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.MEDIUM)
                        .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.HEURISTIC)
                        .sourceText(sanitized)
                        .build());
                return true;
            }
        }

        return false;
    }

    // ── Phase B — run AFTER education / occupation heuristics ────────────────

    /**
     * Attempts to detect an unlabeled candidate name at the top of the document.
     *
     * <p>
     * Guards (all must pass):
     * <ul>
     * <li>Full name not yet set.</li>
     * <li>Not inside a family block and not inside braces.</li>
     * <li>Line has no {@code :} {@code -} {@code ,} {@code @} and no digits.</li>
     * <li>1–4 words; does not contain "details", "biodata", "profile", "born",
     * "ft".</li>
     * <li>Line does not start with (or equal) any known field alias.</li>
     * </ul>
     *
     * @return {@code true} if a candidate name was set; the caller should
     *         {@code continue}.
     */
    public boolean tryExtractUnlabeledName(String sanitized, String lowerLine, ParseContext ctx) {
        if (ctx.profile.getFullName() != null || ctx.inFamilyBlock || ctx.braceDepth != 0) {
            return false;
        }
        if (sanitized.contains(":") || sanitized.contains("-") || sanitized.contains(",")
                || sanitized.contains("@") || sanitized.matches(".*\\d.*")
                || BiodataParserUtils.isIgnorableLine(sanitized)) {
            return false;
        }

        String[] words = sanitized.split("\\s+");
        if (words.length < 1 || words.length > 4
                || lowerLine.contains("details") || lowerLine.contains("biodata")
                || lowerLine.contains("profile") || lowerLine.contains("born")
                || lowerLine.contains("ft") || lowerLine.contains("confidential")
                || lowerLine.contains("page")
                || lowerLine.contains("అమ్మాయి") || lowerLine.contains("అబ్బాయి")
                || lowerLine.contains("bride") || lowerLine.contains("groom")
                || lowerLine.contains("match") || lowerLine.contains("మ్యాచ్")
                || lowerLine.contains("సంబంధం")) {
            return false;
        }

        // Reject if the line starts with (or exactly equals) any known field alias
        for (BiodataField f : BiodataField.values()) {
            for (String alias : f.getAliases()) {
                if (lowerLine.startsWith(alias + " ") || lowerLine.equals(alias)) {
                    return false;
                }
            }
        }

        ctx.profile.setFullName(sanitized);
        ctx.givenName = sanitized;
        ctx.givenNameIsHeuristic = true;
        ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                .field(BiodataField.FULL_NAME)
                .value(sanitized)
                .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.MEDIUM)
                .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.HEURISTIC)
                .sourceText(sanitized)
                .build());
        return true;
    }
}
