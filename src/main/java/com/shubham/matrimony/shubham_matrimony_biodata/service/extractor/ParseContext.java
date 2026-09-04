package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mutable state passed through all extractor classes during a single
 * parse run.
 *
 * <p>
 * Previously these were 15+ local variables scattered across
 * {@code parseBiodata()}.
 * Grouping them here lets each extractor read and update state without needing
 * to
 * return complex objects or use method parameters for every variable.
 */
public class ParseContext {

    /**
     * Tracks which family member's sub-block is currently being parsed.
     * Prevents a father's "Farmer" occupation from overwriting the candidate's
     * "Software Engineer".
     */
    public enum FamilySection {
        NONE, FATHER, MOTHER, SIBLING, OTHER_FAMILY
    }

    // ── Core output ──────────────────────────────────────────────────────────
    /** The profile being populated across all extractors. */
    public final ProfileBiodata profile = new ProfileBiodata();

    /** Lines that could not be matched to any field. */
    public final List<String> unparsedLines = new ArrayList<>();

    /** Diagnostic warnings produced during parsing (reserved for future use). */
    public final List<String> warnings = new ArrayList<>();

    /** Atomic evidence collected across all extractors for Version 2 Merger. */
    public final List<com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult> evidenceList = new ArrayList<>();

    /** Current entity scope/ownership in the document (defaults to CANDIDATE). */
    public com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext currentContext = com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext.CANDIDATE;

    /** Emits a single piece of extracted evidence to the evidence pool. */
    public void emit(com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult evidence) {
        if (evidence != null) {
            evidenceList.add(evidence);
        }
    }

    // ── State machine ────────────────────────────────────────────────────────
    /** Whether the parser is currently inside a family details block. */
    public boolean inFamilyBlock = false;

    /** Whether the parser is currently inside a properties / assets block. */
    public boolean inPropertiesBlock = false;

    /** Whether the parser is currently inside a grandparents block. */
    public boolean inGrandparentsBlock = false;

    /**
     * Which family member is currently in scope (Father / Mother / Sibling / …).
     */
    public FamilySection section = FamilySection.NONE;

    /**
     * JSON-style brace depth counter; prevents scope confusion in JSON-like inputs.
     */
    public int braceDepth = 0;

    /**
     * The brace depth at which the family block was entered.
     * When braceDepth drops below this value the family block is exited.
     * -1 means no family block has been entered via brace counting.
     */
    public int familyBraceDepth = -1;

    /**
     * Set to a {@link BiodataField} when the parser enters a multi-line JSON array
     * (e.g. {@code "education": ["B.Tech", "PGD"]}). Cleared when the array closes.
     */
    public BiodataField inArrayField = null;

    // ── Name buffers ─────────────────────────────────────────────────────────
    /**
     * Surname / last-name captured separately; merged into fullName in
     * post-processing.
     */
    public String surname = null;

    /**
     * Given name captured from a "Name:" label; merged with surname in
     * post-processing.
     */
    public String givenName = null;

    // ── Sibling accumulator ──────────────────────────────────────────────────
    /**
     * All completed sibling entries formatted as "Elder Brother: Rohil (Software
     * Engineer)".
     */
    public final List<String> siblingEntries = new ArrayList<>();

    /**
     * Relation type of the sibling currently being parsed (e.g. "Elder Brother").
     */
    public String currentSiblingRelation = null;

    /** Name of the sibling currently being parsed. */
    public String currentSiblingName = null;

    /** Job/occupation of the sibling currently being parsed. */
    public String currentSiblingJob = null;
}
