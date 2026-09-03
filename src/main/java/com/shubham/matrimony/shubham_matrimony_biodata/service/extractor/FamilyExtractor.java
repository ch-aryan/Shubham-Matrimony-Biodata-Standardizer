package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import java.util.List;

/**
 * Handles the accumulation, classification, and flushing of sibling records,
 * and routes parsed segments into the correct parent or sibling family fields.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Parsing compound parent lines:
 * {@code "Father Ravinder - COO Embedded IT"}.</li>
 * <li>Detecting standalone father-job keywords inside the FATHER section.</li>
 * <li>Detecting standalone "homemaker"/"housewife" inside the MOTHER
 * section.</li>
 * <li>Routing {@code ParsedSegment} fields (FATHER_NAME, MOTHER_NAME, SIBLINGS,
 * etc.)
 * to the right family member based on {@link ParseContext#section}.</li>
 * <li>Classifying sibling relationships from keywords (English + Telugu).</li>
 * <li>Building and flushing formatted sibling entries into
 * {@link ParseContext#siblingEntries}.</li>
 * </ul>
 */
public class FamilyExtractor {

    // ── Standalone compound parent line heuristics ───────────────────────────

    /**
     * Detects and parses unlabeled compound parent lines like
     * {@code "Father Ravinder - COO Embedded IT"} or
     * {@code "Mother Vanitha - Home Maker"}.
     *
     * <p>
     * These appear without an explicit "Father Name:" label — they begin with
     * "father "
     * or "mother " and contain a dash separator.
     *
     * @return {@code true} if the line was consumed; the caller should
     *         {@code continue}.
     */
    public boolean tryExtractCompoundParentLine(String sanitized, String lowerLine, ParseContext ctx) {
        if (lowerLine.startsWith("father ") && lowerLine.contains(" - ")) {
            String[] parts = sanitized.split("\\s+-\\s+", 2);
            int spaceIdx = parts[0].indexOf(' ');
            String pName = spaceIdx != -1 ? parts[0].substring(spaceIdx + 1).trim() : parts[0].trim();
            ctx.profile.setFatherName(pName);
            if (parts.length > 1) {
                ctx.profile.setFatherOccupation(parts[1].trim());
            }
            return true;
        }
        if (lowerLine.startsWith("mother ") && lowerLine.contains(" - ")) {
            String[] parts = sanitized.split("\\s+-\\s+", 2);
            int spaceIdx = parts[0].indexOf(' ');
            String mName = spaceIdx != -1 ? parts[0].substring(spaceIdx + 1).trim() : parts[0].trim();
            ctx.profile.setMotherName(mName);
            if (parts.length > 1) {
                ctx.profile.setMotherOccupation(parts[1].trim());
            }
            return true;
        }
        return false;
    }

    /**
     * Detects a standalone job title keyword when the parser is in the FATHER
     * section
     * and no father occupation has been set yet
     * (e.g. a line reading just {@code "COO Embedded IT"} after a "Father" header).
     *
     * @return {@code true} if the line was consumed; the caller should
     *         {@code continue}.
     */
    public boolean tryExtractStandaloneFatherJob(String sanitized, ParseContext ctx) {
        if (!ctx.inFamilyBlock || ctx.section != ParseContext.FamilySection.FATHER)
            return false;
        if (ctx.profile.getFatherOccupation() != null && !ctx.profile.getFatherOccupation().isBlank())
            return false;
        String lower = sanitized.toLowerCase();
        if (lower.startsWith("coo") || lower.startsWith("ceo") || lower.startsWith("manager")
                || lower.startsWith("engineer") || lower.startsWith("developer")
                || lower.startsWith("business") || lower.startsWith("teacher")
                || lower.startsWith("farmer") || lower.startsWith("govt")
                || lower.startsWith("officer") || lower.startsWith("director")
                || lower.startsWith("consultant") || lower.startsWith("employee")
                || lower.startsWith("advocate") || lower.startsWith("doctor")) {
            ctx.profile.setFatherOccupation(sanitized);
            return true;
        }
        return false;
    }

    /**
     * Detects a standalone "homemaker" / "housewife" / "గృహిణి" inside the MOTHER
     * section.
     *
     * @return {@code true} if the line was consumed; the caller should
     *         {@code continue}.
     */
    public boolean tryExtractStandaloneMotherOccupation(String sanitized, ParseContext ctx) {
        if (!ctx.inFamilyBlock)
            return false;
        if (ctx.section != ParseContext.FamilySection.MOTHER
                && ctx.section != ParseContext.FamilySection.OTHER_FAMILY)
            return false;
        if (sanitized.equalsIgnoreCase("homemaker") || sanitized.equalsIgnoreCase("housewife")
                || sanitized.equalsIgnoreCase("home maker") || sanitized.equals("గృహిణి")) {
            if (ctx.profile.getMotherOccupation() == null || ctx.profile.getMotherOccupation().isBlank()) {
                ctx.profile.setMotherOccupation(sanitized);
                return true;
            }
        }
        return false;
    }

    // ── ParsedSegment routing ────────────────────────────────────────────────

    /**
     * Attempts to apply a {@code ParsedSegment} field to the appropriate family
     * member.
     *
     * <p>
     * Handles (in order):
     * <ol>
     * <li>FATHER_NAME — with optional inline {@code "(job)"} or {@code "– job"}
     * suffix.</li>
     * <li>FATHER_OCCUPATION</li>
     * <li>MOTHER_NAME — with optional inline {@code "(job)"} or {@code "– job"}
     * suffix.</li>
     * <li>MOTHER_OCCUPATION</li>
     * <li>NATIVE_PLACE / CURRENT_LOCATION — always candidate-level, not
     * family.</li>
     * <li>FULL_NAME inside FATHER/MOTHER/SIBLING sections.</li>
     * <li>SIBLINGS field inside family block.</li>
     * <li>Any other field inside a family block — silently dropped to prevent
     * family data from overwriting candidate fields.</li>
     * </ol>
     *
     * @return {@code true} if the segment was consumed; the caller should
     *         {@code continue}.
     *         Returns {@code false} for candidate-level fields outside a family
     *         block.
     */
    public boolean tryApply(com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField field,
            String value, ParseContext ctx) {

        // ── Explicit father fields ──────────────────────────────────────────
        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.FATHER_NAME) {
            String fName = value;
            String fJob = null;
            if (value.contains(" (") && value.endsWith(")")) {
                int pIdx = value.lastIndexOf(" (");
                if (pIdx >= 0 && pIdx + 2 <= value.length() - 1) {
                    fName = value.substring(0, pIdx).trim();
                    fJob = value.substring(pIdx + 2, value.length() - 1).trim();
                }
            } else if (value.contains(" - ")) {
                String[] parts = value.split("\\s+-\\s+", 2);
                fName = parts[0].trim();
                fJob = parts[1].trim();
            }
            if (ctx.profile.getFatherName() == null || ctx.profile.getFatherName().isBlank()) {
                ctx.profile.setFatherName(fName);
            }
            if (fJob != null
                    && (ctx.profile.getFatherOccupation() == null || ctx.profile.getFatherOccupation().isBlank())) {
                ctx.profile.setFatherOccupation(fJob);
            }
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.FATHER;
            return true;
        }

        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.FATHER_OCCUPATION) {
            if (ctx.profile.getFatherOccupation() == null || ctx.profile.getFatherOccupation().isBlank()) {
                ctx.profile.setFatherOccupation(value);
            }
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.FATHER;
            return true;
        }

        // ── Explicit mother fields ──────────────────────────────────────────
        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.MOTHER_NAME) {
            String mName = value;
            String mJob = null;
            if (value.contains(" (") && value.endsWith(")")) {
                int pIdx = value.lastIndexOf(" (");
                if (pIdx >= 0 && pIdx + 2 <= value.length() - 1) {
                    mName = value.substring(0, pIdx).trim();
                    mJob = value.substring(pIdx + 2, value.length() - 1).trim();
                }
            } else if (value.contains(" - ")) {
                String[] parts = value.split("\\s+-\\s+", 2);
                mName = parts[0].trim();
                mJob = parts[1].trim();
            }
            if (ctx.profile.getMotherName() == null || ctx.profile.getMotherName().isBlank()) {
                ctx.profile.setMotherName(mName);
            }
            if (mJob != null
                    && (ctx.profile.getMotherOccupation() == null || ctx.profile.getMotherOccupation().isBlank())) {
                ctx.profile.setMotherOccupation(mJob);
            }
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.MOTHER;
            return true;
        }

        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.MOTHER_OCCUPATION) {
            if (ctx.profile.getMotherOccupation() == null || ctx.profile.getMotherOccupation().isBlank()) {
                ctx.profile.setMotherOccupation(value);
            }
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.MOTHER;
            return true;
        }

        // ── Location fields — always candidate-level, even inside family block ──
        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.NATIVE_PLACE) {
            if (ctx.profile.getNativePlace() == null || ctx.profile.getNativePlace().isBlank()) {
                ctx.profile.setNativePlace(value);
            }
            return true;
        }

        if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.CURRENT_LOCATION) {
            if (ctx.profile.getCurrentLocation() == null || ctx.profile.getCurrentLocation().isBlank()) {
                ctx.profile.setCurrentLocation(value);
            }
            return true;
        }

        // ── Inside a family block: route generic fields to the right member ──
        if (ctx.inFamilyBlock) {
            if (ctx.section == ParseContext.FamilySection.FATHER) {
                if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.FULL_NAME
                        && (ctx.profile.getFatherName() == null || ctx.profile.getFatherName().isBlank())) {
                    ctx.profile.setFatherName(value);
                    return true;
                }
            } else if (ctx.section == ParseContext.FamilySection.MOTHER) {
                if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.FULL_NAME
                        && (ctx.profile.getMotherName() == null || ctx.profile.getMotherName().isBlank())) {
                    ctx.profile.setMotherName(value);
                    return true;
                }
            } else if (ctx.section == ParseContext.FamilySection.SIBLING) {
                if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.FULL_NAME
                        && ctx.currentSiblingName == null) {
                    ctx.currentSiblingName = value;
                    return true;
                } else if (field == com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField.SIBLINGS) {
                    ctx.siblingEntries.add(value);
                    return true;
                }
            }
            // Drop any other field inside a family block to prevent it from
            // overwriting candidate-level properties.
            return true;
        }

        // Candidate-level — let PropertyExtractor handle it
        return false;
    }

    // ── Sibling accumulation ─────────────────────────────────────────────────

    /**
     * Flushes the currently buffered sibling (relation + name + job) into
     * {@link ParseContext#siblingEntries}.
     */
    public void flushCurrentSibling(ParseContext ctx) {
        flushSibling(ctx.siblingEntries,
                ctx.currentSiblingRelation,
                ctx.currentSiblingName,
                ctx.currentSiblingJob);
    }

    /**
     * Builds a formatted sibling string and appends it to {@code entries}
     * (deduped).
     *
     * <p>
     * Format: {@code <Relation>: <Name> (<Job>)}
     * e.g. {@code "Elder Brother: Rohil Thota (Software Engineer)"}.
     *
     * @param entries  accumulation list
     * @param relation canonical relation string (e.g. "Elder Brother")
     * @param name     sibling's name
     * @param job      sibling's occupation
     */
    public void flushSibling(List<String> entries, String relation, String name, String job) {
        if (relation != null || name != null || job != null) {
            StringBuilder sb = new StringBuilder();
            if (relation != null) {
                sb.append(relation);
            }
            if (name != null && !name.isBlank()) {
                if (sb.length() > 0)
                    sb.append(": ");
                sb.append(name);
            }
            if (job != null && !job.isBlank()) {
                if (sb.length() > 0)
                    sb.append(" (").append(job).append(")");
                else
                    sb.append(job);
            }
            String entry = sb.toString().trim();
            if (!entry.isBlank() && !entries.contains(entry)) {
                entries.add(entry);
            }
        }
    }

    /**
     * Classifies a sibling relationship from a lowercased line containing
     * English or Telugu (transliterated / script) keywords.
     *
     * @param lowerLine lowercased line text
     * @return canonical English relation string (e.g. "Elder Brother", "Sister",
     *         "Sibling")
     */
    public String extractSiblingRelation(String lowerLine) {
        if (lowerLine.contains("brother_in_law") || lowerLine.contains("brother in law"))
            return "Brother-in-law";
        if (lowerLine.contains("sister_in_law") || lowerLine.contains("sister in law"))
            return "Sister-in-law";
        if (lowerLine.contains("elder_sister") || lowerLine.contains("elder sister")
                || lowerLine.contains("అక్క"))
            return "Elder Sister";
        if (lowerLine.contains("younger_sister") || lowerLine.contains("younger sister")
                || lowerLine.contains("చెల్లెలు"))
            return "Younger Sister";
        if (lowerLine.contains("elder_brother") || lowerLine.contains("elder brother")
                || lowerLine.contains("అన్న"))
            return "Elder Brother";
        if (lowerLine.contains("younger_brother") || lowerLine.contains("younger brother")
                || lowerLine.contains("తమ్ముడు"))
            return "Younger Brother";
        if (lowerLine.contains("sister") || lowerLine.contains("అక్కచెల్లెళ్ళు"))
            return "Sister";
        if (lowerLine.contains("brother") || lowerLine.contains("అన్నదమ్ములు")
                || lowerLine.contains("సోదరులు"))
            return "Brother";
        return "Sibling";
    }
}
