package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

/**
 * Applies candidate-level labeled field segments that are not handled by any
 * of the specialized extractors ({@link FamilyExtractor},
 * {@link OccupationExtractor}, etc.).
 *
 * <p>
 * Handles:
 * <ul>
 * <li>{@code SURNAME} — buffered separately; merged with {@code givenName} in
 * post-processing.</li>
 * <li>{@code FULL_NAME} — buffered as {@code givenName}; longest-wins when seen
 * multiple times.</li>
 * <li>{@code SIBLINGS} — candidate-level sibling count/description at top
 * level.</li>
 * <li>All remaining fields — generic first-wins apply via
 * {@link BiodataField#apply(com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata, String)}.</li>
 * </ul>
 *
 * <p>
 * This extractor is the <em>last</em> step in the segment-dispatch chain and is
 * only
 * called when {@link FamilyExtractor#tryApply} returned {@code false} (i.e. the
 * segment
 * belongs to the candidate, not a family member).
 */
public class PropertyExtractor {

    /**
     * Applies a candidate-level segment to the profile stored in {@code ctx}.
     *
     * <p>
     * Assumes the caller has already verified the segment is <em>not</em> a family
     * field
     * (i.e. {@link FamilyExtractor#tryApply} returned {@code false}).
     *
     * @param field the canonical field enum constant
     * @param value the extracted string value
     * @param ctx   shared parse context
     */
    public void apply(BiodataField field, String value, ParseContext ctx) {

        // SURNAME is buffered and merged with givenName in post-processing
        if (field == BiodataField.SURNAME) {
            if (ctx.surname == null) {
                ctx.surname = value;
                ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                        .field(field)
                        .value(value)
                        .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                        .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                        .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                        .sourceText(value)
                        .build());
            }
            return;
        }

        // FULL_NAME: keep the longest value that contains the previous one (handles
        // "Aryan" followed by "Aryan Kumar" → prefer the more complete form)
        if (field == BiodataField.FULL_NAME) {
            boolean accepted = false;
            if (ctx.givenName == null || ctx.givenNameIsHeuristic) {
                ctx.givenName = value;
                ctx.givenNameIsHeuristic = false;
                accepted = true;
            } else if (value.length() > ctx.givenName.length()
                    && value.toLowerCase().contains(ctx.givenName.toLowerCase())) {
                ctx.givenName = value;
                accepted = true;
            }
            if (accepted) {
                ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                        .field(field)
                        .value(value)
                        .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                        .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                        .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                        .sourceText(value)
                        .build());
            }
            return;
        }

        // Candidate-level siblings (e.g. "2 Brothers, 1 Sister")
        if (field == BiodataField.SIBLINGS) {
            if (!ctx.siblingEntries.contains(value)) {
                ctx.siblingEntries.add(value);
                ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                        .field(field)
                        .value(value)
                        .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.SIBLING)
                        .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                        .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                        .sourceText(value)
                        .build());
            }
            return;
        }

        // Generic first-wins: apply only if the field has not been set yet
        String currentVal = field.getGetter().apply(ctx.profile);
        if (currentVal == null || currentVal.isBlank()) {
            field.apply(ctx.profile, value);
            ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                    .field(field)
                    .value(value)
                    .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                    .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                    .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                    .sourceText(value)
                    .build());
        }
        ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                .field(field)
                .value(value)
                .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE)
                .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                .sourceText(value)
                .build());
    }

    /**
     * Attempts to dynamically capture an unrecognized key-value line into
     * {@link com.shubham.matrimony.shubham_matrimony_biodata.dto.AdditionalInformation#getCustomAttributes()}.
     *
     * <p>Prevents arbitrary non-canonical fields (e.g. {@code "Diet: Pure Vegetarian"},
     * {@code "Requirements :- Only Software Engineer"}, {@code "Visa status - Permanent Resident GC"},
     * {@code "Earnings - $ 130 + Stocks"}) from being lost or requiring rigid schema changes.
     * <p>
     * Prevents arbitrary non-canonical fields (e.g.
     * {@code "Diet: Pure Vegetarian"},
     * {@code "Requirements :- Only Software Engineer"},
     * {@code "Visa status - Permanent Resident GC"},
     * {@code "Earnings - $ 130 + Stocks"}) from being lost or requiring rigid
     * schema changes.
     * <p>
     * Prevents arbitrary non-canonical fields (e.g.
     * {@code "Diet: Pure Vegetarian"},
     * {@code "Requirements :- Only Software Engineer"},
     * {@code "Visa status - Permanent Resident GC"},
     * {@code "Earnings - $ 130 + Stocks"}) from being lost or requiring rigid
     * schema changes.
     *
     * @param line sanitized input line
     * @param ctx  shared parse context
     * @return {@code true} if captured as a key-value attribute; {@code false} otherwise.
     * @return {@code true} if captured as a key-value attribute; {@code false}
     *         otherwise.
     * @return {@code true} if captured as a key-value attribute; {@code false}
     *         otherwise.
     */
    public boolean tryCaptureCustomAttribute(String line, ParseContext ctx) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String trimmed = line.trim();
        String[] parts = null;
        if (trimmed.contains(":-")) {
            parts = trimmed.split(":-", 2);
        } else if (trimmed.contains(":")) {
            parts = trimmed.split(":", 2);
        } else if (trimmed.contains(" - ")) {
            parts = trimmed.split("\\s+-\\s+", 2);
        } else if (trimmed.contains("=")) {
            parts = trimmed.split("=", 2);
        }

        if (parts != null && parts.length == 2) {
            String key = parts[0].replaceAll("^[\\s\\*\\•\\-\\–\\—#\\.\"]+", "")
                                 .replaceAll("[\\s\\*\\•\\-\\–\\—#\\.\"]+$", "")
                                 .trim();
                    .replaceAll("[\\s\\*\\•\\-\\–\\—#\\.\"]+$", "")
                    .trim();
            String val = parts[1].replaceAll("^[\\s:=–—~\\|/\\-\\.\\*\\•#\\)\\]\\}\"\'`]+", "")
                                 .replaceAll("[\\s,\\|;~–—\\-\\.\\*\\•#\\(\\[\\{\\\"\'`]+$", "")
                                 .trim();
                    .replaceAll("[\\s,\\|;~–—\\-\\.\\*\\•#\\(\\[\\{\\\"\'`]+$", "")
                    .trim();

            if (!key.isBlank() && !val.isBlank() && key.length() <= 50
                    && !key.toLowerCase().contains("http") && !key.contains("/")) {
                String[] words = key.split("\\s+");
                if (words.length <= 6 && !key.endsWith(".")) {
                    ctx.profile.getAdditionalInfo().getCustomAttributes().put(key, val);
                    ctx.profile.getAdditionalInfo().addCustomAttribute(key, val,
                            com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH,
                            com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC,
                            line);
                    ctx.emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult.builder()
                            .field(null)
                            .field(com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.CUSTOM_ATTRIBUTE)
                            .value(val)
                            .context(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.OTHER)
                            .confidence(com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence.HIGH)
                            .method(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod.DETERMINISTIC)
                            .sourceText(line)
                            .build());
                    return true;
                }
            }
        }
        return false;
    }
}
