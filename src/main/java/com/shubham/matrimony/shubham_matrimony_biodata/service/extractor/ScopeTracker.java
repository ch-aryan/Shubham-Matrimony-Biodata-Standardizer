package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

/**
 * Maintains the brace-depth and family-block scope in {@link ParseContext}.
 *
 * <p>The tracker is called in <strong>two phases</strong> per line to honour the exact
 * order the original single-class engine used:
 *
 * <ol>
 *   <li>{@link #updateEarly(String, String, ParseContext)} — <em>before</em> the
 *       ignorable/conversational skip checks.
 *       Resets {@code inArrayField} on {@code ]}, counts braces, and detects
 *       family-block entry and exit.</li>
 *   <li>{@link #updateLate(String, String, ParseContext)} — <em>after</em> the
 *       multi-line array item check.
 *       Transitions the family sub-section (Father / Mother / Sibling) and
 *       flushes a completed sibling record when a closing brace is encountered.</li>
 * </ol>
 */
public class ScopeTracker {

    private final FamilyExtractor familyExtractor;

    /**
     * @param familyExtractor used by the late-update phase to flush/classify siblings
     */
    public ScopeTracker(FamilyExtractor familyExtractor) {
        this.familyExtractor = familyExtractor;
    }

    // ── Phase 1 ───────────────────────────────────────────────────────────────

    /**
     * Early scope update — call this <em>before</em> the ignorable/conversational checks.
     *
     * <p>Actions (in order):
     * <ol>
     *   <li>Resets {@code ctx.inArrayField} when {@code rawLine} contains {@code ]}.</li>
     *   <li>Increments / decrements brace depth (clamped at 0 to guard malformed input).</li>
     *   <li>Enters the family block when a family-header keyword is detected.</li>
     *   <li>Exits the family block when brace depth falls below the entry depth.</li>
     *   <li>Exits the family block when a section-exit keyword is detected
     *       (partner preference, contact details, disclaimer, etc.).</li>
     * </ol>
     *
     * @param rawLine   original unsanitized line (used for {@code ]} and brace counting)
     * @param lowerLine lowercased sanitized line (used for keyword matching)
     * @param ctx       shared parse context to update
     */
    public void updateEarly(String rawLine, String lowerLine, ParseContext ctx) {
        // Reset multi-line array tracking on closing bracket
        if (rawLine.contains("]")) {
            ctx.inArrayField = null;
        }

        // Track JSON brace depth (clamp to 0 to handle malformed unbalanced braces)
        for (char c : rawLine.toCharArray()) {
            if (c == '{')
                ctx.braceDepth++;
            else if (c == '}')
                ctx.braceDepth = Math.max(0, ctx.braceDepth - 1);
        }

        // Enter family block on family-header keywords
        if (lowerLine.contains("family") || lowerLine.contains("family_details")
                || lowerLine.contains("కుటుంబ వివరాలు") || lowerLine.contains("కుటుంబం")
                || lowerLine.contains("కుటుంబ నేపథ్యం")) {
            ctx.inFamilyBlock = true;
            ctx.familyBraceDepth = ctx.braceDepth;
            ctx.section = ParseContext.FamilySection.OTHER_FAMILY;

        // Exit family block when brace depth drops below the depth at which we entered
        } else if (ctx.familyBraceDepth != -1 && ctx.braceDepth < ctx.familyBraceDepth) {
            ctx.inFamilyBlock = false;
            ctx.familyBraceDepth = -1;
            ctx.section = ParseContext.FamilySection.NONE;

        // Exit family block on section-exit keywords (partner preferences, contact, etc.)
        } else if (ctx.braceDepth <= 0
                && (lowerLine.contains("partner") || lowerLine.contains("preference")
                        || lowerLine.contains("references") || lowerLine.contains("disclaimer")
                        || lowerLine.contains("contact") || lowerLine.contains("contact details")
                        || lowerLine.contains("native_place") || lowerLine.contains("native place")
                        || lowerLine.contains("settled_location") || lowerLine.contains("settled location")
                        || lowerLine.contains("జీవిత భాగస్వామి") || lowerLine.contains("జీవిత_భాగస్వామి")
                        || lowerLine.contains("సూచనలు") || lowerLine.contains("గమనిక"))) {
            ctx.inFamilyBlock = false;
            ctx.familyBraceDepth = -1;
            ctx.section = ParseContext.FamilySection.NONE;
        }
    }

    // ── Phase 2 ───────────────────────────────────────────────────────────────

    /**
     * Late scope update — call this <em>after</em> the multi-line array item check
     * and <em>before</em> the standalone heuristics.
     *
     * <p>Actions (in order):
     * <ol>
     *   <li>Switches sub-section to OTHER_FAMILY on grandparent keywords.</li>
     *   <li>Switches sub-section to FATHER on father keywords.</li>
     *   <li>Switches sub-section to MOTHER on mother keywords.</li>
     *   <li>Switches sub-section to SIBLING on sibling keywords, flushing the
     *       previously buffered sibling record first.</li>
     *   <li>Flushes a completed sibling record when a closing brace line is seen
     *       while the parser is in the SIBLING section.</li>
     * </ol>
     *
     * @param sanitized sanitized current line
     * @param lowerLine lowercased sanitized line
     * @param ctx       shared parse context to update
     */
    public void updateLate(String sanitized, String lowerLine, ParseContext ctx) {
        if (lowerLine.contains("grandparent") || lowerLine.contains("grandfather")
                || lowerLine.contains("grandmother") || lowerLine.contains("తాత")
                || lowerLine.contains("అమ్మమ్మ") || lowerLine.contains("నానమ్మ")
                || lowerLine.contains("paternal") || lowerLine.contains("maternal")) {
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.OTHER_FAMILY;

        } else if (lowerLine.contains("father") || lowerLine.contains("thandri")
                || lowerLine.contains("తండ్రి") || lowerLine.contains("నాన్న")) {
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.FATHER;

        } else if (lowerLine.contains("mother") || lowerLine.contains("thalli")
                || lowerLine.contains("తల్లి")
                || (lowerLine.contains("అమ్మ")
                        && !lowerLine.contains("అమ్మమ్మ")
                        && !lowerLine.contains("అమ్మాయి"))) {
            ctx.inFamilyBlock = true;
            ctx.section = ParseContext.FamilySection.MOTHER;

        } else if (lowerLine.matches(".*\\b(sister|brother|siblings?|anna|thammudu|akka|chellelu|annayya)\\b.*")
                || lowerLine.contains("తోబుట్టువులు") || lowerLine.contains("సోదరులు")
                || lowerLine.contains("అన్నదమ్ములు") || lowerLine.contains("అక్కచెల్లెళ్ళు")
                || lowerLine.contains("అన్నయ్య")) {
            ctx.inFamilyBlock = true;
            // Flush the previously buffered sibling before starting a new one
            familyExtractor.flushCurrentSibling(ctx);
            ctx.currentSiblingRelation = familyExtractor.extractSiblingRelation(lowerLine);
            ctx.currentSiblingName = null;
            ctx.currentSiblingJob = null;
            ctx.section = ParseContext.FamilySection.SIBLING;
        }

        // Flush completed sibling record on closing brace inside a SIBLING section
        if (ctx.section == ParseContext.FamilySection.SIBLING
                && (sanitized.startsWith("}") || sanitized.startsWith("},"))) {
            familyExtractor.flushCurrentSibling(ctx);
            ctx.currentSiblingRelation = null;
            ctx.currentSiblingName = null;
            ctx.currentSiblingJob = null;
        }
    }
}
