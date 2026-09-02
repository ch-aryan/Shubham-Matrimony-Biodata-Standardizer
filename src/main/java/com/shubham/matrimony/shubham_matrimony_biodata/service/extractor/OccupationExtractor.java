package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataLabels;

/**
 * Extracts occupation and company for the candidate (not family members).
 *
 * <p>Handles two patterns:
 * <ol>
 *   <li>{@link #tryExtractStandalone} — unlabeled lines containing {@code " @ "}
 *       (e.g. {@code "Asst Manager @ CIBC Mellon"}).</li>
 *   <li>{@link #applyOccupationSegment} — labeled {@code OCCUPATION} segments that may
 *       embed a company via {@code " at "} or {@code " @ "}, with routing to
 *       candidate or the current family member.</li>
 * </ol>
 */
public class OccupationExtractor {

    // ── Standalone @ pattern ──────────────────────────────────────────────────

    /**
     * Detects and sets a standalone {@code "Role @ Company"} pattern that appears
     * without an explicit occupation label.
     *
     * <p>Strips any leading occupation alias prefix (e.g. {@code "Job - "} or
     * {@code "Profession: "}) so only the clean role title is stored.
     *
     * <p>Guards: occupation not yet set, not inside a family block, no colon, no comma,
     * line must contain {@code " @ "}.
     *
     * @return {@code true} if the line was consumed; the caller should {@code continue}.
     */
    public boolean tryExtractStandalone(String sanitized, String lowerLine, ParseContext ctx) {
        if (ctx.profile.getOccupation() != null
                || ctx.inFamilyBlock
                || sanitized.contains(":")
                || sanitized.contains(",")
                || !sanitized.contains(" @ ")) {
            return false;
        }
        String[] parts = sanitized.split("\\s+@\\s+", 2);
        String role = parts[0].trim();

        // Strip leading occupation alias prefix (e.g. "Job - " before the actual role)
        for (String alias : BiodataLabels.OCCUPATION) {
            if (role.toLowerCase().startsWith(alias)) {
                String after = role.substring(alias.length()).trim();
                if (after.startsWith("-") || after.startsWith(":") || after.startsWith("–")
                        || after.startsWith("—") || after.startsWith("~")) {
                    role = after.substring(1).trim();
                    break;
                }
            }
        }

        ctx.profile.setOccupation(role);
        if (ctx.profile.getCompany() == null || ctx.profile.getCompany().isBlank()) {
            ctx.profile.setCompany(parts[1].trim());
        }
        return true;
    }

    // ── Labeled OCCUPATION segment ────────────────────────────────────────────

    /**
     * Applies an {@code OCCUPATION} {@link com.shubham.matrimony.shubham_matrimony_biodata.util.ParsedSegment}
     * value, splitting embedded {@code " at "} / {@code " @ "} to extract company.
     *
     * <p>Routing rules:
     * <ul>
     *   <li>Inside a FATHER section → sets father's occupation.</li>
     *   <li>Inside a MOTHER section → sets mother's occupation.</li>
     *   <li>Inside a SIBLING section → sets the current sibling's job buffer.</li>
     *   <li>Candidate scope → sets the candidate's occupation (first-wins).</li>
     * </ul>
     *
     * @param value raw segment value extracted by the label scanner
     * @param ctx   shared parse context
     */
    public void applyOccupationSegment(String value, ParseContext ctx) {
        String cleanRole = value;

        if (value.contains(" at ")) {
            String[] parts = value.split("\\s+at\\s+", 2);
            cleanRole = parts[0].trim();
            if (!ctx.inFamilyBlock
                    && (ctx.profile.getCompany() == null || ctx.profile.getCompany().isBlank())
                    && parts.length > 1) {
                ctx.profile.setCompany(parts[1].trim());
            }
        } else if (value.contains(" @ ")) {
            String[] parts = value.split("\\s+@\\s+", 2);
            cleanRole = parts[0].trim();
            if (!ctx.inFamilyBlock
                    && (ctx.profile.getCompany() == null || ctx.profile.getCompany().isBlank())
                    && parts.length > 1) {
                ctx.profile.setCompany(parts[1].trim());
            }
        }

        if (ctx.inFamilyBlock) {
            switch (ctx.section) {
                case FATHER:
                    if (ctx.profile.getFatherOccupation() == null
                            || ctx.profile.getFatherOccupation().isBlank()) {
                        ctx.profile.setFatherOccupation(cleanRole);
                    }
                    break;
                case MOTHER:
                    if (ctx.profile.getMotherOccupation() == null
                            || ctx.profile.getMotherOccupation().isBlank()) {
                        ctx.profile.setMotherOccupation(cleanRole);
                    }
                    break;
                case SIBLING:
                    if (ctx.currentSiblingJob == null) {
                        ctx.currentSiblingJob = cleanRole;
                    }
                    break;
                default:
                    break;
            }
        } else {
            if (ctx.profile.getOccupation() == null || ctx.profile.getOccupation().isBlank()) {
                ctx.profile.setOccupation(cleanRole);
            }
        }
    }
}
