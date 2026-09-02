package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataLabels;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils.ParsedSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BiodataParserImplementation implements BiodataServiceParser {

    /**
     * Internal State Machine tracking which family member context we are currently
     * in.
     * Prevents family member attributes (like Father's job "Farmer") from polluting
     * the candidate's personal attributes (like Candidate's job "Software
     * Engineer").
     */
    private enum FamilySection {
        NONE, FATHER, MOTHER, SIBLING, OTHER_FAMILY
    }

    /**
     * Simplified convenience entry point that returns only the populated
     * {@link ProfileBiodata}.
     *
     * @param rawText The raw unformatted biodata string copied from WhatsApp, text,
     *                or PDF.
     * @return The populated ProfileBiodata object.
     */
    @Override
    public ProfileBiodata parse(String rawText) {
        return parseBiodata(rawText).getProfile();
    }

    /**
     * Main Core Parsing Engine Method.
     * <p>
     * How the Pipeline works step-by-step:
     * 1. Pre-processing: Splits input by line breaks, flattens pipe-separated lines
     * ("|").
     * 2. Sanitization: Strips WhatsApp timestamps and invisible Unicode artifacts.
     * 3. State Machine & Scoping: Tracks JSON braces and Family block sections
     * (Father, Mother, Siblings).
     * 4. Pattern Recognition: Checks standalone patterns (Height, Suffix Born,
     * Degree prefixes, Jobs with '@').
     * 5. Interval Disjoint Extraction: Identifies known labels, sorts by
     * priority/length, extracts boundary-safe values.
     * 6. Candidate vs Family Mapping: Assigns values to the candidate or redirects
     * them to the proper family member.
     * 7. Post-Processing: Merges Surname + Given Name, aggregates siblings, and
     * computes confidence scores.
     *
     * @param rawText The raw unformatted biodata text.
     * @return ExtractionResultDTO containing profile, confidence scores, warnings,
     *         and unparsed lines.
     */
    @Override
    public ExtractionResultDTO parseBiodata(String rawText) {
        ProfileBiodata profile = new ProfileBiodata();
        Map<String, FieldConfidence> confidenceScores = new HashMap<>();
        List<String> unparsedLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            populateMissingConfidence(confidenceScores);
            return ExtractionResultDTO.builder()
                    .profile(profile)
                    .confidenceScores(confidenceScores)
                    .unparsedLines(unparsedLines)
                    .warnings(warnings)
                    .build();
        }

        String surname = null;
        String givenName = null;
        boolean inFamilyBlock = false;
        FamilySection section = FamilySection.NONE;
        List<String> siblingEntries = new ArrayList<>();
        String currentSiblingRelation = null;
        String currentSiblingName = null;
        String currentSiblingJob = null;

        BiodataField inArrayField = null;
        int braceDepth = 0;
        int familyBraceDepth = -1;
        List<String> rawLines = new ArrayList<>();
        for (String line : rawText.split("\\r?\\n")) {
            if (line.contains("|")) {
                for (String part : line.split("\\|")) {
                    if (!part.isBlank()) {
                        rawLines.add(part.trim());
                    }
                }
            } else {
                rawLines.add(line);
            }
        }
        for (String rawLine : rawLines) {
            String sanitized = BiodataParserUtils.sanitizeLine(rawLine);
            if (rawLine.contains("]")) {
                inArrayField = null;
            }

            for (char c : rawLine.toCharArray()) {
                if (c == '{')
                    braceDepth++;
                else if (c == '}')
                    braceDepth = Math.max(0, braceDepth - 1); // Guard against malformed unbalanced braces
            }

            String lowerLine = sanitized.toLowerCase().trim();

            // Detect entering/exiting main blocks dynamically based on family opening depth
            if (lowerLine.contains("family") || lowerLine.contains("family_details")
                    || lowerLine.contains("కుటుంబ వివరాలు") || lowerLine.contains("కుటుంబం")
                    || lowerLine.contains("కుటుంబ నేపథ్యం")) {
                inFamilyBlock = true;
                familyBraceDepth = braceDepth;
                section = FamilySection.OTHER_FAMILY;
            } else if (familyBraceDepth != -1 && braceDepth < familyBraceDepth) {
                inFamilyBlock = false;
                familyBraceDepth = -1;
                section = FamilySection.NONE;
            } else if (braceDepth <= 0
                    && (lowerLine.contains("partner") || lowerLine.contains("preference")
                            || lowerLine.contains("references") || lowerLine.contains("disclaimer")
                            || lowerLine.contains("contact") || lowerLine.contains("contact details")
                            || lowerLine.contains("native_place") || lowerLine.contains("native place")
                            || lowerLine.contains("settled_location") || lowerLine.contains("settled location")
                            || lowerLine.contains("జీవిత భాగస్వామి") || lowerLine.contains("జీవిత_భాగస్వామి")
                            || lowerLine.contains("సూచనలు") || lowerLine.contains("గమనిక"))) {
                inFamilyBlock = false;
                familyBraceDepth = -1;
                section = FamilySection.NONE;
            }

            if (BiodataParserUtils.isIgnorableLine(sanitized)) {
                continue;
            }

            if (BiodataParserUtils.isConversationalNote(lowerLine)) {
                unparsedLines.add(sanitized);
                continue;
            }

            // Multi-line JSON array handling (e.g. "విద్య": [ "BBA", "PGD..." ])
            if (inArrayField != null) {
                if (sanitized.contains("]") || sanitized.contains("{") || sanitized.contains(":")) {
                    inArrayField = null;
                } else {
                    String item = BiodataParserUtils.cleanValue(sanitized);
                    if (!item.isBlank() && !item.equals(",")) {
                        String existing = profile.getQualification();
                        if (existing == null || existing.isBlank()) {
                            profile.setQualification(item);
                        } else if (!existing.contains(item)) {
                            profile.setQualification(existing + ", " + item);
                        }
                    }
                    continue;
                }
            }

            // Track sections within family
            if (lowerLine.contains("grandparent") || lowerLine.contains("grandfather")
                    || lowerLine.contains("grandmother") || lowerLine.contains("తాత")
                    || lowerLine.contains("అమ్మమ్మ") || lowerLine.contains("నానమ్మ")
                    || lowerLine.contains("paternal") || lowerLine.contains("maternal")) {
                inFamilyBlock = true;
                section = FamilySection.OTHER_FAMILY;
            } else if (lowerLine.contains("father") || lowerLine.contains("thandri") || lowerLine.contains("తండ్రి")
                    || lowerLine.contains("నాన్న")) {
                inFamilyBlock = true;
                section = FamilySection.FATHER;
            } else if (lowerLine.contains("mother") || lowerLine.contains("thalli")
                    || lowerLine.contains("తల్లి") || (lowerLine.contains("అమ్మ") && !lowerLine.contains("అమ్మమ్మ")
                            && !lowerLine.contains("అమ్మాయి"))) {
                inFamilyBlock = true;
                section = FamilySection.MOTHER;
            } else if (lowerLine.matches(".*\\b(sister|brother|siblings?|anna|thammudu|akka|chellelu|annayya)\\b.*")
                    || lowerLine.contains("తోబుట్టువులు") || lowerLine.contains("సోదరులు")
                    || lowerLine.contains("అన్నదమ్ములు") || lowerLine.contains("అక్కచెల్లెళ్ళు")
                    || lowerLine.contains("అన్నయ్య")) {
                inFamilyBlock = true;
                flushSibling(siblingEntries, currentSiblingRelation, currentSiblingName, currentSiblingJob);
                currentSiblingRelation = extractSiblingRelation(lowerLine);
                currentSiblingName = null;
                currentSiblingJob = null;
                section = FamilySection.SIBLING;
            }

            if (section == FamilySection.SIBLING && (sanitized.startsWith("}") || sanitized.startsWith("},"))) {
                flushSibling(siblingEntries, currentSiblingRelation, currentSiblingName, currentSiblingJob);
                currentSiblingRelation = null;
                currentSiblingName = null;
                currentSiblingJob = null;
            }

            // Standalone Height pattern (e.g. "6ft", "5'9", "5.8", "5ft 10in")
            if (profile.getHeight() == null && !sanitized.contains(":") && !sanitized.contains(",")
                    && sanitized.matches(
                            "(?i)^[4-7]\\s*('|\"|ft|feet|\\.)(\\s*\\d{1,2}(\"|in|inches)?)?$|^[4-7]\\s*(ft|feet)$")) {
                profile.setHeight(sanitized);
                continue;
            }

            // Suffix born pattern (e.g. "Nizamabad born", "Hyderabad born")
            if (!sanitized.contains(":")
                    && (sanitized.toLowerCase().endsWith(" born") || sanitized.toLowerCase().endsWith(" born."))) {
                String place = sanitized.substring(0, sanitized.toLowerCase().lastIndexOf(" born")).trim();
                if (place.toLowerCase().startsWith("at ") || place.toLowerCase().startsWith("in ")) {
                    place = place.substring(3).trim();
                }
                if (profile.getPlaceOfBirth() == null || profile.getPlaceOfBirth().isBlank()) {
                    profile.setPlaceOfBirth(place);
                    continue;
                }
            }

            // Standalone Education prefix (e.g. "B.Tech JNTU", "MBA IIM", "PGD in Financial
            // Planning")
            if (!inFamilyBlock && !sanitized.contains(":") && !sanitized.contains(",")) {
                String lower = sanitized.toLowerCase();
                if (lower.startsWith("b.tech") || lower.startsWith("m.tech") || lower.startsWith("bba")
                        || lower.startsWith("mba") || lower.startsWith("ms ") || lower.startsWith("mbbs")
                        || lower.startsWith("b.sc") || lower.startsWith("b.com") || lower.startsWith("be ")
                        || lower.startsWith("b.e.") || lower.startsWith("diploma") || lower.startsWith("degree")
                        || lower.startsWith("pgd") || lower.startsWith("pg diploma")) {
                    if (profile.getQualification() == null || profile.getQualification().isBlank()) {
                        profile.setQualification(sanitized);
                    } else if (!profile.getQualification().contains(sanitized)) {
                        profile.setQualification(profile.getQualification() + ", " + sanitized);
                    }
                    continue;
                }
            }

            // Standalone Job with @ (e.g. "Asst Manager @ CIBC Mellon" or "Job - Financial
            // Analyst @ Deloitte")
            if (profile.getOccupation() == null && !inFamilyBlock && !sanitized.contains(":")
                    && !sanitized.contains(",")
                    && sanitized.contains(" @ ")) {
                String[] parts = sanitized.split("\\s+@\\s+", 2);
                String role = parts[0].trim();
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
                profile.setOccupation(role);
                if (profile.getCompany() == null || profile.getCompany().isBlank()) {
                    profile.setCompany(parts[1].trim());
                }
                continue;
            }

            // Compound Parent Line with delimiter (e.g. "Father Ravinder - COO Embedded
            // IT", "Mother Vanitha - Home Maker")
            if (lowerLine.startsWith("father ") && lowerLine.contains(" - ")) {
                String[] parts = sanitized.split("\\s+-\\s+", 2);
                int spaceIdx = parts[0].indexOf(' ');
                String pName = spaceIdx != -1 ? parts[0].substring(spaceIdx + 1).trim() : parts[0].trim();
                profile.setFatherName(pName);
                if (parts.length > 1) {
                    profile.setFatherOccupation(parts[1].trim());
                }
                continue;
            }
            if (lowerLine.startsWith("mother ") && lowerLine.contains(" - ")) {
                String[] parts = sanitized.split("\\s+-\\s+", 2);
                int spaceIdx = parts[0].indexOf(' ');
                String mName = spaceIdx != -1 ? parts[0].substring(spaceIdx + 1).trim() : parts[0].trim();
                profile.setMotherName(mName);
                if (parts.length > 1) {
                    profile.setMotherOccupation(parts[1].trim());
                }
                continue;
            }

            // Unlabeled Candidate Name at Top of Document
            // If fullName is unset, no colons/digits, 1-4 words, and early in document
            if (profile.getFullName() == null && !inFamilyBlock && braceDepth == 0) {
                if (!sanitized.contains(":") && !sanitized.contains("-") && !sanitized.contains(",")
                        && !sanitized.contains("@")
                        && !sanitized.matches(".*\\d.*") && !BiodataParserUtils.isIgnorableLine(sanitized)) {
                    String[] words = sanitized.split("\\s+");
                    if (words.length >= 1 && words.length <= 4 && !sanitized.toLowerCase().contains("details")
                            && !sanitized.toLowerCase().contains("biodata")
                            && !sanitized.toLowerCase().contains("profile")
                            && !sanitized.toLowerCase().contains("born") && !sanitized.toLowerCase().contains("ft")) {
                        boolean isLabel = false;
                        for (BiodataField f : BiodataField.values()) {
                            for (String alias : f.getAliases()) {
                                if (sanitized.toLowerCase().startsWith(alias + " ")
                                        || sanitized.toLowerCase().equals(alias)) {
                                    isLabel = true;
                                    break;
                                }
                            }
                            if (isLabel)
                                break;
                        }
                        if (!isLabel) {
                            profile.setFullName(sanitized);
                            continue;
                        }
                    }
                }
            }

            // Check for standalone job inside father section (e.g. "COO Embedded IT")
            if (inFamilyBlock && section == FamilySection.FATHER
                    && (profile.getFatherOccupation() == null || profile.getFatherOccupation().isBlank())) {
                String lower = sanitized.toLowerCase();
                if (lower.startsWith("coo") || lower.startsWith("ceo") || lower.startsWith("manager")
                        || lower.startsWith("engineer") || lower.startsWith("developer") || lower.startsWith("business")
                        || lower.startsWith("teacher") || lower.startsWith("farmer") || lower.startsWith("govt")
                        || lower.startsWith("officer") || lower.startsWith("director") || lower.startsWith("consultant")
                        || lower.startsWith("employee") || lower.startsWith("advocate") || lower.startsWith("doctor")) {
                    profile.setFatherOccupation(sanitized);
                    continue;
                }
            }

            // Check for standalone homemaker / housewife inside family
            if (inFamilyBlock && (section == FamilySection.MOTHER || section == FamilySection.OTHER_FAMILY)) {
                if (sanitized.equalsIgnoreCase("homemaker") || sanitized.equalsIgnoreCase("housewife")
                        || sanitized.equalsIgnoreCase("home maker") || sanitized.equals("గృహిణి")) {
                    if (profile.getMotherOccupation() == null || profile.getMotherOccupation().isBlank()) {
                        profile.setMotherOccupation(sanitized);
                    }
                    continue;
                }
            }

            // If a line starts a multi-line array of qualification/education strings (e.g.
            // "విద్య": [)
            if (sanitized.contains("[") && !sanitized.contains("]") && !sanitized.contains("{")) {
                String beforeBracket = sanitized.substring(0, sanitized.indexOf('['));
                for (String alias : BiodataLabels.QUALIFICATION) {
                    if (beforeBracket.toLowerCase().contains(alias)) {
                        inArrayField = BiodataField.QUALIFICATION;
                        break;
                    }
                }
                continue;
            }

            List<ParsedSegment> segments = BiodataParserUtils.parseTextSegments(sanitized);
            if (segments.isEmpty()) {
                if (!BiodataParserUtils.isIgnorableLine(sanitized)) {
                    unparsedLines.add(sanitized);
                }
                continue;
            }

            for (ParsedSegment segment : segments) {
                BiodataField field = segment.getField();
                String value = segment.getValue();

                // Explicit Parental Fields (always mapped to parents whether in family block or
                // not)
                if (field == BiodataField.FATHER_NAME) {
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
                    if (profile.getFatherName() == null || profile.getFatherName().isBlank()) {
                        profile.setFatherName(fName);
                    }
                    if (fJob != null
                            && (profile.getFatherOccupation() == null || profile.getFatherOccupation().isBlank())) {
                        profile.setFatherOccupation(fJob);
                    }
                    inFamilyBlock = true;
                    section = FamilySection.FATHER;
                    continue;
                }
                if (field == BiodataField.FATHER_OCCUPATION) {
                    if (profile.getFatherOccupation() == null || profile.getFatherOccupation().isBlank()) {
                        profile.setFatherOccupation(value);
                    }
                    inFamilyBlock = true;
                    section = FamilySection.FATHER;
                    continue;
                }
                if (field == BiodataField.MOTHER_NAME) {
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
                    if (profile.getMotherName() == null || profile.getMotherName().isBlank()) {
                        profile.setMotherName(mName);
                    }
                    if (mJob != null
                            && (profile.getMotherOccupation() == null || profile.getMotherOccupation().isBlank())) {
                        profile.setMotherOccupation(mJob);
                    }
                    inFamilyBlock = true;
                    section = FamilySection.MOTHER;
                    continue;
                }
                if (field == BiodataField.MOTHER_OCCUPATION) {
                    if (profile.getMotherOccupation() == null || profile.getMotherOccupation().isBlank()) {
                        profile.setMotherOccupation(value);
                    }
                    inFamilyBlock = true;
                    section = FamilySection.MOTHER;
                    continue;
                }
                if (field == BiodataField.NATIVE_PLACE) {
                    if (profile.getNativePlace() == null || profile.getNativePlace().isBlank()) {
                        profile.setNativePlace(value);
                    }
                    continue;
                }
                if (field == BiodataField.CURRENT_LOCATION) {
                    if (profile.getCurrentLocation() == null || profile.getCurrentLocation().isBlank()) {
                        profile.setCurrentLocation(value);
                    }
                    continue;
                }

                // Split "Role at Company" (e.g. "Assistant Manager at CIBC Mellon")
                if (field == BiodataField.OCCUPATION) {
                    String cleanRole = value;
                    if (value.contains(" at ")) {
                        String[] parts = value.split("\\s+at\\s+", 2);
                        cleanRole = parts[0].trim();
                        if (!inFamilyBlock && (profile.getCompany() == null || profile.getCompany().isBlank())
                                && parts.length > 1) {
                            profile.setCompany(parts[1].trim());
                        }
                    } else if (value.contains(" @ ")) {
                        String[] parts = value.split("\\s+@\\s+", 2);
                        cleanRole = parts[0].trim();
                        if (!inFamilyBlock && (profile.getCompany() == null || profile.getCompany().isBlank())
                                && parts.length > 1) {
                            profile.setCompany(parts[1].trim());
                        }
                    }

                    if (inFamilyBlock) {
                        if (section == FamilySection.FATHER
                                && (profile.getFatherOccupation() == null || profile.getFatherOccupation().isBlank())) {
                            profile.setFatherOccupation(cleanRole);
                        } else if (section == FamilySection.MOTHER
                                && (profile.getMotherOccupation() == null || profile.getMotherOccupation().isBlank())) {
                            profile.setMotherOccupation(cleanRole);
                        } else if (section == FamilySection.SIBLING && currentSiblingJob == null) {
                            currentSiblingJob = cleanRole;
                        }
                    } else {
                        if (profile.getOccupation() == null || profile.getOccupation().isBlank()) {
                            profile.setOccupation(cleanRole);
                        }
                    }
                    continue;
                }

                if (inFamilyBlock) {
                    if (section == FamilySection.FATHER) {
                        if (field == BiodataField.FULL_NAME
                                && (profile.getFatherName() == null || profile.getFatherName().isBlank())) {
                            profile.setFatherName(value);
                            continue;
                        }
                    } else if (section == FamilySection.MOTHER) {
                        if (field == BiodataField.FULL_NAME
                                && (profile.getMotherName() == null || profile.getMotherName().isBlank())) {
                            profile.setMotherName(value);
                            continue;
                        }
                    } else if (section == FamilySection.SIBLING) {
                        if (field == BiodataField.FULL_NAME && currentSiblingName == null) {
                            currentSiblingName = value;
                            continue;
                        } else if (field == BiodataField.SIBLINGS) {
                            siblingEntries.add(value);
                            continue;
                        }
                    }
                    // Prevent family details from overwriting candidate-level properties!
                    continue;
                }

                // CANDIDATE LEVEL PROCESSING:
                if (field == BiodataField.SURNAME) {
                    if (surname == null) {
                        surname = value;
                    }
                    continue;
                }

                if (field == BiodataField.FULL_NAME) {
                    if (givenName == null) {
                        givenName = value;
                    } else if (value.length() > givenName.length()
                            && value.toLowerCase().contains(givenName.toLowerCase())) {
                        givenName = value;
                    }
                    continue;
                }

                if (field == BiodataField.SIBLINGS) {
                    siblingEntries.add(value);
                    continue;
                }

                String currentVal = field.getGetter().apply(profile);
                if (currentVal == null || currentVal.isBlank()) {
                    field.apply(profile, value);
                }
            }
        }

        // Flush last parsed sibling entry
        flushSibling(siblingEntries, currentSiblingRelation, currentSiblingName, currentSiblingJob);

        // Populate siblings details if aggregated
        if ((profile.getSiblingsDetails() == null || profile.getSiblingsDetails().isBlank())
                && !siblingEntries.isEmpty()) {
            profile.setSiblingsDetails(String.join(", ", siblingEntries));
        }

        // Finalize candidate's Full Name
        if (surname != null && !surname.isBlank() && givenName != null && !givenName.isBlank()) {
            if (!givenName.toLowerCase().contains(surname.toLowerCase())) {
                profile.setFullName(surname + " " + givenName);
            } else {
                profile.setFullName(givenName);
            }
        } else if (givenName != null && !givenName.isBlank()) {
            profile.setFullName(givenName);
        } else if (surname != null && !surname.isBlank()) {
            profile.setFullName(surname);
        }

        // Compute confidence scores for every canonical field
        for (BiodataField field : BiodataField.values()) {
            if (field == BiodataField.SURNAME) {
                continue; // internal helper field, represented under fullName
            }
            String val = field.getGetter().apply(profile);
            if (val != null && !val.isBlank()) {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.HIGH);
            } else {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.MISSING);
            }
        }

        return ExtractionResultDTO.builder()
                .profile(profile)
                .confidenceScores(confidenceScores)
                .unparsedLines(unparsedLines)
                .warnings(warnings)
                .build();
    }

    /**
     * Initializes confidence scores for all supported biodata fields to MISSING.
     * Used as a baseline or fallback when raw input is blank or invalid.
     *
     * @param confidenceScores The target map to populate with MISSING statuses.
     */
    private void populateMissingConfidence(Map<String, FieldConfidence> confidenceScores) {
        for (BiodataField field : BiodataField.values()) {
            if (field != BiodataField.SURNAME) {
                confidenceScores.put(field.getPropertyName(), FieldConfidence.MISSING);
            }
        }
    }

    /**
     * Detects specific sibling relationships in English or Telugu (transliterated
     * or Telugu script).
     * <p>
     * Recognizes terms like "Elder Sister", "Younger Brother", "అక్క", "తమ్ముడు",
     * "Brother-in-law", etc., and standardizes them into a canonical English
     * relation name.
     *
     * @param lowerLine Lowercased text containing potential sibling keywords.
     * @return Canonical relation string (e.g., "Elder Brother", "Sister",
     *         "Sibling").
     */
    private String extractSiblingRelation(String lowerLine) {
        if (lowerLine.contains("brother_in_law") || lowerLine.contains("brother in law"))
            return "Brother-in-law";
        if (lowerLine.contains("sister_in_law") || lowerLine.contains("sister in law"))
            return "Sister-in-law";
        if (lowerLine.contains("elder_sister") || lowerLine.contains("elder sister") || lowerLine.contains("అక్క"))
            return "Elder Sister";
        if (lowerLine.contains("younger_sister") || lowerLine.contains("younger sister")
                || lowerLine.contains("చెల్లెలు"))
            return "Younger Sister";
        if (lowerLine.contains("elder_brother") || lowerLine.contains("elder brother") || lowerLine.contains("అన్న"))
            return "Elder Brother";
        if (lowerLine.contains("younger_brother") || lowerLine.contains("younger brother")
                || lowerLine.contains("తమ్ముడు"))
            return "Younger Brother";
        if (lowerLine.contains("sister") || lowerLine.contains("అక్కచెల్లెళ్ళు"))
            return "Sister";
        if (lowerLine.contains("brother") || lowerLine.contains("అన్నదమ్ములు") || lowerLine.contains("సోదరులు"))
            return "Brother";
        return "Sibling";
    }

    /**
     * Aggregates and flushes a buffered sibling record into the entries list.
     * <p>
     * Formats the pieces together as:
     * {@code <Relation>: <Name> (<Job>)} -> e.g., "Elder Brother: Rohil Thota
     * (Software Engineer)".
     * Avoids duplicates and ignores empty entries.
     *
     * @param entries  Target list where formatted sibling entries are stored.
     * @param relation Sibling relation (e.g. "Elder Brother").
     * @param name     Sibling's name (e.g. "Rohil Thota").
     * @param job      Sibling's profession/occupation (e.g. "Software Engineer").
     */
    private void flushSibling(List<String> entries, String relation, String name, String job) {
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
}