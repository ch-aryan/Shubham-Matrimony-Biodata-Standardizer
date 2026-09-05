package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

/**
 * Dedicated extractor for Horoscope attributes: Rashi, Nakshatram, and Gothram.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Cleans trailing redundant label suffixes (e.g. {@code "Thula rashi"} →
 * {@code "Thula"},
 * {@code "Makha nakshatram"} → {@code "Makha"}).</li>
 * <li>Handles compound horoscope lines where both Rashi and Nakshatram appear
 * in a single segment
 * (e.g. {@code "Simha rasi, Makha nakshatram"}).</li>
 * <li>Emits atomic {@link ExtractionResult} evidence into
 * {@link ParseContext#emit} while preserving
 * direct profile mutation for backwards compatibility during migration.</li>
 * </ul>
 */
public class HoroscopeExtractor {

    /**
     * Attempts to process and apply a horoscope segment.
     *
     * @param field the candidate field enum
     * @param value the extracted segment value
     * @param ctx   shared parse context
     * @return {@code true} if this segment was handled as a horoscope field;
     *         {@code false} otherwise.
     */
    public boolean tryApply(BiodataField field, String value, ParseContext ctx) {
        if (field == BiodataField.RASHI) {
            applyRashi(value, ctx);
            return true;
        }

        if (field == BiodataField.NAKSHATRAM) {
            applyNakshatram(value, ctx);
            return true;
        }

        if (field == BiodataField.GOTHRAM) {
            applyGothram(value, ctx);
            return true;
        }

        return false;
    }

    private void applyRashi(String value, ParseContext ctx) {
        // Check if value contains both Rashi and Nakshatram (e.g. "Simha rasi, Makha
        // nakshatram")
        if (value.contains(",") || value.toLowerCase().contains("nakshatra") || value.toLowerCase().contains("star")
                || value.toLowerCase().contains("nakhsathram")) {
            String[] parts = value.split("[,&|]+");
            for (String part : parts) {
                String trimmed = part.trim();
                String lower = trimmed.toLowerCase();
                if (lower.contains("nakshatra") || lower.contains("star") || lower.contains("nakhsathram")) {
                    applyNakshatram(trimmed, ctx);
                } else if (!trimmed.isBlank()) {
                    setCleanRashi(trimmed, ctx);
                }
            }
            return;
        }

        setCleanRashi(value, ctx);
    }

    private void setCleanRashi(String value, ParseContext ctx) {
        String cleaned = cleanRashiSuffix(value);
        if (!cleaned.isBlank()) {
            if (ctx.profile.getRashi() == null || ctx.profile.getRashi().isBlank()) {
                ctx.profile.setRashi(cleaned);
            }
            ctx.emit(ExtractionResult.builder()
                    .field(BiodataField.RASHI)
                    .value(cleaned)
                    .context(ExtractionContext.CANDIDATE)
                    .confidence(FieldConfidence.HIGH)
                    .method(ExtractionMethod.DETERMINISTIC)
                    .sourceText(value)
                    .build());
        }
    }

    private void applyNakshatram(String value, ParseContext ctx) {
        // Check if value contains both Nakshatram and Rashi
        if (value.contains(",") && (value.toLowerCase().contains("rashi") || value.toLowerCase().contains("rasi"))) {
            String[] parts = value.split("[,&|]+");
            for (String part : parts) {
                String trimmed = part.trim();
                String lower = trimmed.toLowerCase();
                if (lower.contains("rashi") || lower.contains("rasi")) {
                    setCleanRashi(trimmed, ctx);
                } else if (!trimmed.isBlank()) {
                    setCleanNakshatram(trimmed, ctx);
                }
            }
            return;
        }

        setCleanNakshatram(value, ctx);
    }

    private void setCleanNakshatram(String value, ParseContext ctx) {
        String cleaned = cleanNakshatramSuffix(value);
        if (!cleaned.isBlank()) {
            if (ctx.profile.getNakshatram() == null || ctx.profile.getNakshatram().isBlank()) {
                ctx.profile.setNakshatram(cleaned);
            }
            ctx.emit(ExtractionResult.builder()
                    .field(BiodataField.NAKSHATRAM)
                    .value(cleaned)
                    .context(ExtractionContext.CANDIDATE)
                    .confidence(FieldConfidence.HIGH)
                    .method(ExtractionMethod.DETERMINISTIC)
                    .sourceText(value)
                    .build());
        }
    }

    private void applyGothram(String value, ParseContext ctx) {
        String cleaned = cleanGothramSuffix(value);
        if (!cleaned.isBlank()) {
            if (ctx.profile.getGothram() == null || ctx.profile.getGothram().isBlank()) {
                ctx.profile.setGothram(cleaned);
            }
            ctx.emit(ExtractionResult.builder()
                    .field(BiodataField.GOTHRAM)
                    .value(cleaned)
                    .context(ExtractionContext.CANDIDATE)
                    .confidence(FieldConfidence.HIGH)
                    .method(ExtractionMethod.DETERMINISTIC)
                    .sourceText(value)
                    .build());
        }
    }

    private String cleanRashiSuffix(String val) {
        String trimmed = val.trim();
        // Remove trailing "rashi", "rasi", "raasi", "sign"
        String cleaned = trimmed.replaceAll("(?i)\\s+(rashi|rasi|raasi|sign|రాశి)$", "").trim();
        // Remove leading "rashi:", "rasi:", etc. if present
        cleaned = cleaned.replaceAll("(?i)^(rashi|rasi|raasi|రాశి)[:\\s-]+", "").trim();
        return cleaned.isBlank() ? trimmed : cleaned;
    }

    private String cleanNakshatramSuffix(String val) {
        String trimmed = val.trim();
        // Remove trailing "nakshatram", "nakshathram", "nakhsathram", "nakshtram",
        // "nakshatra", "star", etc.
        String cleaned = trimmed
                .replaceAll("(?i)\\s+(nakshatram|nakshathram|nakhsathram|nakshtram|nakshatra|star|నక్షత్రం)$", "")
                .trim();
        // Remove leading "star:", "nakshatram:", etc. if present
        cleaned = cleaned
                .replaceAll("(?i)^(nakshatram|nakshathram|nakhsathram|nakshtram|nakshatra|star|నక్షత్రం)[:\\s-]+", "")
                .trim();
        return cleaned.isBlank() ? trimmed : cleaned;
    }

    private String cleanGothramSuffix(String val) {
        String trimmed = val.trim();
        // Remove trailing "gotram", "gothram", "gotra", "గోత్రం"
        String cleaned = trimmed.replaceAll("(?i)\\s+(gotram|gothram|gotra|గోత్రం)$", "").trim();
        // Remove leading "gotram:", "gothram:", etc. if present
        cleaned = cleaned.replaceAll("(?i)^(gotram|gothram|gotra|గోత్రం)[:\\s-]+", "").trim();
        return cleaned.isBlank() ? trimmed : cleaned;
    }
}
