package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.AdditionalInformation;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts non-canonical matrimonial details (Properties, Grandparents, Visas,
 * Physical attributes, Lifestyle/Hobbies, Marital Status) into
 * {@link AdditionalInformation}.
 *
 * <p>
 * Prevents these valuable domain details from being dropped into
 * {@code unparsedLines},
 * without requiring dozens of new database columns.
 */
@Component
public class AdditionalInfoExtractor {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(?i)^(?:weight|wt)[:\\s-]+([0-9]+(?:\\.[0-9]+)?\\s*(?:kgs?|lbs?|kilos?)?)");

    private static final Pattern COMPLEXION_PATTERN = Pattern.compile(
            "(?i)^(?:complexion|skin\\s*tone)[:\\s-]+([a-z\\s]+)$");

    private static final Pattern MARITAL_STATUS_PATTERN = Pattern.compile(
            "(?i)^(?:marital\\s*status)[:\\s-]+([a-z\\s]+)$");

    private static final Pattern VISA_PATTERN = Pattern.compile(
            "(?i)^(?:visa\\s*status|visa)[:\\s-]+(.+)$");

    private static final Pattern HOBBIES_PATTERN = Pattern.compile(
            "(?i)^(?:hobbies|hobby|interests)[:\\s-]+(.+)$");

    private static final Pattern RELIGION_PATTERN = Pattern.compile(
            "(?i)^(?:religion)[:\\s-]+([a-z\\s]+)$");

    private static final Pattern MOTHER_TONGUE_PATTERN = Pattern.compile(
            "(?i)^(?:mother\\s*tongue)[:\\s-]+([a-z\\s]+)$");

    private static final Pattern RESIDENCE_PATTERN = Pattern.compile(
            "(?i)^(?:residence)[:\\s-]+(.+)$");

    private static final Pattern COUNTRY_PATTERN = Pattern.compile(
            "(?i)^(?:country)[:\\s-]+([a-z\\s]+)$");

    private static final Pattern PARTNER_PREFERENCES_PATTERN = Pattern.compile(
            "(?i)^(?:partner\\s*preferences?|partner\\s*expectations?)\\s*[:=–—~-]+\\s*(.+)$");

    /**
     * Attempts to extract an additional-info segment or multi-line
     * property/grandparent item.
     *
     * @param line sanitized line of text
     * @param ctx  shared parse context
     * @return {@code true} if this line was captured as additional info;
     *         {@code false} otherwise.
     */
    public boolean tryExtract(String line, ParseContext ctx) {
        String trimmed = line.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        String lower = trimmed.toLowerCase();
        AdditionalInformation info = ctx.profile.getAdditionalInfo();

        // ── 1. Section Header Detection ───────────────────────────────────────
        if (lower.matches(
                "(?i)^(properties|property|property\\s*details|assets|property\\s*/\\s*assets(?:\\s*details)?|ఆస్తులు)[:\\s-]*$")) {
            ctx.inPropertiesBlock = true;
            ctx.inGrandparentsBlock = false;
            ctx.inPartnerPreferencesBlock = false;
            return true;
        }

        if (lower.matches(
                "(?i)^(grandparents|grandparents\\s*details|paternal\\s*grandparents?|maternal\\s*grandparents?)[:\\s-]*$")) {
            ctx.inGrandparentsBlock = true;
            ctx.inPropertiesBlock = false;
            ctx.inPartnerPreferencesBlock = false;
            return true;
        }

        if (lower.matches(
                "(?i)^(partner\\s*preferences?|partner\\s*expectations?)[:\\s-]*$")) {
            ctx.inPartnerPreferencesBlock = true;
            ctx.inPropertiesBlock = false;
            ctx.inGrandparentsBlock = false;
            return true;
        }

        // Inline property line: e.g. "Properties: Own house G+1 in Shamshabad..." or "Property/Assets: Well-settled family (₹9 Cr)"
        if (lower.matches("(?i)^(?:properties|property|assets|property\\s*/\\s*assets(?:\\s*details)?)\\s*[:=–—~-].*$")) {
            ctx.inPropertiesBlock = true;
            String val = trimmed
                    .replaceFirst("(?i)^(?:property\\s*/\\s*assets(?:\\s*details)?|properties|property|assets)\\s*[:=–—~-]+", "").trim();
            if (!val.isBlank()) {
                info.getProperties().add(val);
                emitEvidence(ExtractionContext.PROPERTY, val, trimmed, ctx);
            }
            return true;
        }

        // Inline grandparent line: e.g. "Paternal Grandparents: Sri Kotte..."
        if (lower.matches("(?i)^(?:paternal\\s*grandparents?['s]*)\\s*[:=–—~-].*$")) {
            String val = trimmed.replaceFirst("(?i)^paternal\\s*grandparents?['s]*\\s*[:=–—~-]+", "").trim();
            if (!val.isBlank()) {
                info.getPaternalGrandparents().add(val);
                emitEvidence(ExtractionContext.GRANDPARENTS, val, trimmed, ctx);
            }
            return true;
        }

        if (lower.matches("(?i)^(?:maternal\\s*grandparents?['s]*)\\s*[:=–—~-].*$")) {
            String val = trimmed.replaceFirst("(?i)^maternal\\s*grandparents?['s]*\\s*[:=–—~-]+", "").trim();
            if (!val.isBlank()) {
                info.getMaternalGrandparents().add(val);
                emitEvidence(ExtractionContext.GRANDPARENTS, val, trimmed, ctx);
            }
            return true;
        }

        // ── 2. Multi-line Block Consumption ───────────────────────────────────
        if (ctx.inPropertiesBlock) {
            // Check if another labeled section started
            if (isOtherSectionHeader(lower)) {
                ctx.inPropertiesBlock = false;
            } else {
                info.getProperties().add(trimmed);
                emitEvidence(ExtractionContext.PROPERTY, trimmed, trimmed, ctx);
                return true;
            }
        }

        if (ctx.inGrandparentsBlock) {
            if (isOtherSectionHeader(lower)) {
                ctx.inGrandparentsBlock = false;
            } else {
                if (lower.contains("paternal")) {
                    // sub-header
                    return true;
                } else if (lower.contains("maternal")) {
                    // sub-header
                    return true;
                }
                info.getPaternalGrandparents().add(trimmed);
                emitEvidence(ExtractionContext.GRANDPARENTS, trimmed, trimmed, ctx);
                return true;
            }
        }

        if (ctx.inPartnerPreferencesBlock) {
            if (isOtherSectionHeader(lower)) {
                ctx.inPartnerPreferencesBlock = false;
            } else {
                String existing = info.getPartnerPreferences();
                if (existing == null || existing.isBlank()) {
                    info.setPartnerPreferences(trimmed);
                } else {
                    info.setPartnerPreferences(existing + "; " + trimmed);
                }
                emitEvidence(ExtractionContext.OTHER, trimmed, trimmed, ctx);
                return true;
            }
        }

        // ── 3. Single-line Non-Canonical Attributes ───────────────────────────
        Matcher m;

        m = WEIGHT_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setWeight(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        m = COMPLEXION_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setComplexion(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        m = MARITAL_STATUS_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            if (ctx.inFamilyBlock || ctx.section == ParseContext.FamilySection.SIBLING) {
                info.getExtendedFamily().add("Marital Status: " + val);
            } else if (info.getMaritalStatus() == null || info.getMaritalStatus().isBlank()) {
                info.setMaritalStatus(val);
                emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            }
            return true;
        }

        m = VISA_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setVisaStatus(val);
            emitEvidence(ExtractionContext.CAREER, val, trimmed, ctx);
            return true;
        }

        m = HOBBIES_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setHobbies(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        m = RELIGION_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setReligion(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        m = MOTHER_TONGUE_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setMotherTongue(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        m = RESIDENCE_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setResidence(val);
            emitEvidence(ExtractionContext.CANDIDATE, val, trimmed, ctx);
            return true;
        }

        m = COUNTRY_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setCountry(val);
            emitEvidence(ExtractionContext.CANDIDATE, val, trimmed, ctx);
            return true;
        }

        m = PARTNER_PREFERENCES_PATTERN.matcher(trimmed);
        if (m.find()) {
            String val = m.group(1).trim();
            info.setPartnerPreferences(val);
            emitEvidence(ExtractionContext.OTHER, val, trimmed, ctx);
            return true;
        }

        return false;
    }

    private boolean isOtherSectionHeader(String lower) {
        return lower.startsWith("family")
                || lower.startsWith("father")
                || lower.startsWith("mother")
                || lower.startsWith("sibling")
                || lower.startsWith("education")
                || lower.startsWith("educational")
                || lower.startsWith("personal")
                || lower.startsWith("name:")
                || lower.startsWith("dob:")
                || lower.startsWith("date of birth")
                || lower.startsWith("references")
                || lower.startsWith("contact")
                || lower.startsWith("disclaimer");
    }

    private void emitEvidence(ExtractionContext context, String value, String sourceText, ParseContext ctx) {
        ctx.emit(ExtractionResult.builder()
                .context(context)
                .value(value)
                .confidence(FieldConfidence.HIGH)
                .method(ExtractionMethod.DETERMINISTIC)
                .sourceText(sourceText)
                .build());
    }
}
