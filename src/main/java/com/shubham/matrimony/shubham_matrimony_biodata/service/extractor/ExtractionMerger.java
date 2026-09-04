package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ConflictRecord;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.EvidenceKey;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.MergeResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reconciles and arbitrates all atomic {@link ExtractionResult} evidence into a canonical {@link ProfileBiodata}.
 *
 * <p>Core Responsibilities:
 * <ol>
 *   <li><b>Context Isolation:</b> Groups evidence by {@link EvidenceKey} (Context + Field), preventing
 *       candidate, father, mother, and sibling data from colliding.</li>
 *   <li><b>Value Deduplication:</b> Identical values from different extraction sources are collapsed cleanly.</li>
 *   <li><b>Conflict Arbitration:</b> Detects contradictory values within the same key. Selects a primary
 *       resolution, flags the field as {@link FieldConfidence#CONFLICT}, and preserves competing evidence.</li>
 *   <li><b>Accumulation:</b> Multi-value fields (e.g. {@code QUALIFICATION}, {@code SIBLINGS}) are combined
 *       rather than treated as conflicts.</li>
 *   <li><b>Composite Assembly:</b> Merges candidate {@code SURNAME} and {@code FULL_NAME} when present.</li>
 *   <li><b>Audit Trail:</b> Preserves the full list of evidence per key for transparency.</li>
 * </ol>
 */
@Component
public class ExtractionMerger {

    /**
     * Merges a collection of extraction evidence into a cohesive {@link MergeResult}.
     *
     * @param evidenceList all atomic observations from all extractors
     * @return populated profile, confidence scores, conflict records, and evidence trail
     */
    public MergeResult merge(List<ExtractionResult> evidenceList) {
        ProfileBiodata profile = new ProfileBiodata();
        Map<String, FieldConfidence> confidenceScores = new HashMap<>();
        List<ConflictRecord> conflicts = new ArrayList<>();
        Map<EvidenceKey, List<ExtractionResult>> evidenceTrail = new LinkedHashMap<>();

        if (evidenceList == null || evidenceList.isEmpty()) {
            markAllMissing(confidenceScores);
            return MergeResult.builder()
                    .profile(profile)
                    .confidenceScores(confidenceScores)
                    .conflicts(conflicts)
                    .evidenceTrail(evidenceTrail)
                    .build();
        }

        // 1. Bucket evidence by (Context, Field)
        for (ExtractionResult item : evidenceList) {
            if (item == null || item.getField() == null || item.getValue() == null || item.getValue().isBlank()) {
                continue;
            }
            ExtractionContext ctx = item.getContext() != null ? item.getContext() : ExtractionContext.ROOT;
            EvidenceKey key = new EvidenceKey(ctx, item.getField());
            evidenceTrail.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        // 2. Candidate Name assembly (Surname + Given Name)
        resolveCandidateName(evidenceTrail, profile, confidenceScores, conflicts);

        // 3. Candidate Core Attributes
        resolveCandidateCoreFields(evidenceTrail, profile, confidenceScores, conflicts);

        // 4. Family Fields (Father & Mother)
        resolveFamilyFields(evidenceTrail, profile, confidenceScores, conflicts);

        // 5. Sibling Details
        resolveSiblings(evidenceTrail, profile, confidenceScores);

        // 6. Mark any unpopulated canonical fields as MISSING
        finalizeConfidenceScores(profile, confidenceScores);

        return MergeResult.builder()
                .profile(profile)
                .confidenceScores(confidenceScores)
                .conflicts(conflicts)
                .evidenceTrail(evidenceTrail)
                .build();
    }

    // ── Private Resolution Methods ───────────────────────────────────────────

    private void resolveCandidateName(Map<EvidenceKey, List<ExtractionResult>> trail,
                                      ProfileBiodata profile,
                                      Map<String, FieldConfidence> confidenceScores,
                                      List<ConflictRecord> conflicts) {
        List<ExtractionResult> surnameEvidence = getEvidence(trail, BiodataField.SURNAME, ExtractionContext.CANDIDATE, ExtractionContext.ROOT);
        List<ExtractionResult> nameEvidence = getEvidence(trail, BiodataField.FULL_NAME, ExtractionContext.CANDIDATE, ExtractionContext.ROOT);

        String surname = resolveSingleValue(surnameEvidence, conflicts, BiodataField.SURNAME, ExtractionContext.CANDIDATE);
        String givenName = resolveSingleValue(nameEvidence, conflicts, BiodataField.FULL_NAME, ExtractionContext.CANDIDATE);

        String fullName = null;
        if (surname != null && !surname.isBlank() && givenName != null && !givenName.isBlank()) {
            if (givenName.toLowerCase().startsWith(surname.toLowerCase())) {
                fullName = givenName;
            } else {
                fullName = surname + " " + givenName;
            }
        } else if (givenName != null && !givenName.isBlank()) {
            fullName = givenName;
        } else if (surname != null && !surname.isBlank()) {
            fullName = surname;
        }

        if (fullName != null && !fullName.isBlank()) {
            profile.setFullName(fullName);
            FieldConfidence conf = highestConfidence(nameEvidence, surnameEvidence);
            if (hasConflictForField(conflicts, BiodataField.FULL_NAME, ExtractionContext.CANDIDATE)) {
                conf = FieldConfidence.CONFLICT;
            }
            confidenceScores.put("fullName", conf);
        }
    }

    private void resolveCandidateCoreFields(Map<EvidenceKey, List<ExtractionResult>> trail,
                                            ProfileBiodata profile,
                                            Map<String, FieldConfidence> confidenceScores,
                                            List<ConflictRecord> conflicts) {
        BiodataField[] candidateFields = {
                BiodataField.DATE_OF_BIRTH,
                BiodataField.TIME_OF_BIRTH,
                BiodataField.PLACE_OF_BIRTH,
                BiodataField.HEIGHT,
                BiodataField.CASTE,
                BiodataField.GOTHRAM,
                BiodataField.RASHI,
                BiodataField.NAKSHATRAM,
                BiodataField.QUALIFICATION,
                BiodataField.OCCUPATION,
                BiodataField.COMPANY,
                BiodataField.SALARY,
                BiodataField.CURRENT_LOCATION,
                BiodataField.NATIVE_PLACE
        };

        for (BiodataField field : candidateFields) {
            List<ExtractionResult> items = getEvidence(trail, field, ExtractionContext.CANDIDATE, ExtractionContext.ROOT);
            if (items.isEmpty()) {
                continue;
            }

            if (field == BiodataField.QUALIFICATION) {
                // Multi-value accumulation
                resolveAccumulativeField(items, field, profile, confidenceScores);
            } else {
                // Single-value resolution with conflict arbitration
                String resolvedValue = resolveSingleValue(items, conflicts, field, ExtractionContext.CANDIDATE);
                if (resolvedValue != null && !resolvedValue.isBlank()) {
                    field.getSetter().accept(profile, resolvedValue);
                    FieldConfidence conf = hasConflictForField(conflicts, field, ExtractionContext.CANDIDATE)
                            ? FieldConfidence.CONFLICT
                            : highestConfidence(items);
                    confidenceScores.put(field.getPropertyName(), conf);
                }
            }
        }
    }

    private void resolveFamilyFields(Map<EvidenceKey, List<ExtractionResult>> trail,
                                     ProfileBiodata profile,
                                     Map<String, FieldConfidence> confidenceScores,
                                     List<ConflictRecord> conflicts) {
        // Father Name
        List<ExtractionResult> fatherNameItems = getEvidence(trail, BiodataField.FULL_NAME, ExtractionContext.FATHER);
        fatherNameItems.addAll(getEvidence(trail, BiodataField.FATHER_NAME, ExtractionContext.FATHER, ExtractionContext.FAMILY, ExtractionContext.ROOT));
        String fatherName = resolveSingleValue(fatherNameItems, conflicts, BiodataField.FATHER_NAME, ExtractionContext.FATHER);
        if (fatherName != null && !fatherName.isBlank()) {
            profile.setFatherName(fatherName);
            FieldConfidence conf = hasConflictForField(conflicts, BiodataField.FATHER_NAME, ExtractionContext.FATHER)
                    ? FieldConfidence.CONFLICT
                    : highestConfidence(fatherNameItems);
            confidenceScores.put("fatherName", conf);
        }

        // Father Occupation
        List<ExtractionResult> fatherJobItems = getEvidence(trail, BiodataField.OCCUPATION, ExtractionContext.FATHER);
        fatherJobItems.addAll(getEvidence(trail, BiodataField.FATHER_OCCUPATION, ExtractionContext.FATHER, ExtractionContext.FAMILY, ExtractionContext.ROOT));
        String fatherJob = resolveSingleValue(fatherJobItems, conflicts, BiodataField.FATHER_OCCUPATION, ExtractionContext.FATHER);
        if (fatherJob != null && !fatherJob.isBlank()) {
            profile.setFatherOccupation(fatherJob);
            FieldConfidence conf = hasConflictForField(conflicts, BiodataField.FATHER_OCCUPATION, ExtractionContext.FATHER)
                    ? FieldConfidence.CONFLICT
                    : highestConfidence(fatherJobItems);
            confidenceScores.put("fatherOccupation", conf);
        }

        // Mother Name
        List<ExtractionResult> motherNameItems = getEvidence(trail, BiodataField.FULL_NAME, ExtractionContext.MOTHER);
        motherNameItems.addAll(getEvidence(trail, BiodataField.MOTHER_NAME, ExtractionContext.MOTHER, ExtractionContext.FAMILY, ExtractionContext.ROOT));
        String motherName = resolveSingleValue(motherNameItems, conflicts, BiodataField.MOTHER_NAME, ExtractionContext.MOTHER);
        if (motherName != null && !motherName.isBlank()) {
            profile.setMotherName(motherName);
            FieldConfidence conf = hasConflictForField(conflicts, BiodataField.MOTHER_NAME, ExtractionContext.MOTHER)
                    ? FieldConfidence.CONFLICT
                    : highestConfidence(motherNameItems);
            confidenceScores.put("motherName", conf);
        }

        // Mother Occupation
        List<ExtractionResult> motherJobItems = getEvidence(trail, BiodataField.OCCUPATION, ExtractionContext.MOTHER);
        motherJobItems.addAll(getEvidence(trail, BiodataField.MOTHER_OCCUPATION, ExtractionContext.MOTHER, ExtractionContext.FAMILY, ExtractionContext.ROOT));
        String motherJob = resolveSingleValue(motherJobItems, conflicts, BiodataField.MOTHER_OCCUPATION, ExtractionContext.MOTHER);
        if (motherJob != null && !motherJob.isBlank()) {
            profile.setMotherOccupation(motherJob);
            FieldConfidence conf = hasConflictForField(conflicts, BiodataField.MOTHER_OCCUPATION, ExtractionContext.MOTHER)
                    ? FieldConfidence.CONFLICT
                    : highestConfidence(motherJobItems);
            confidenceScores.put("motherOccupation", conf);
        }
    }

    private void resolveSiblings(Map<EvidenceKey, List<ExtractionResult>> trail,
                                 ProfileBiodata profile,
                                 Map<String, FieldConfidence> confidenceScores) {
        List<ExtractionResult> siblingItems = getEvidence(trail, BiodataField.SIBLINGS, ExtractionContext.SIBLING, ExtractionContext.FAMILY, ExtractionContext.ROOT);

        // Also check any SIBLING context full-names (e.g. Sibling Name: Rohil)
        List<ExtractionResult> siblingNames = getEvidence(trail, BiodataField.FULL_NAME, ExtractionContext.SIBLING);
        for (ExtractionResult sn : siblingNames) {
            if (!siblingItems.contains(sn)) {
                siblingItems.add(sn);
            }
        }

        if (siblingItems.isEmpty()) {
            return;
        }

        // Deduplicate distinct sibling representations
        Set<String> distinctEntries = new LinkedHashSet<>();
        for (ExtractionResult item : siblingItems) {
            if (item.getValue() != null && !item.getValue().isBlank()) {
                distinctEntries.add(item.getValue().trim());
            }
        }

        if (!distinctEntries.isEmpty()) {
            String combined = String.join(", ", distinctEntries);
            profile.setSiblingsDetails(combined);
            confidenceScores.put("siblingsDetails", highestConfidence(siblingItems));
        }
    }

    private void resolveAccumulativeField(List<ExtractionResult> items,
                                          BiodataField field,
                                          ProfileBiodata profile,
                                          Map<String, FieldConfidence> confidenceScores) {
        Set<String> distinct = new LinkedHashSet<>();
        for (ExtractionResult item : items) {
            String v = item.getValue().trim();
            if (!v.isBlank()) {
                distinct.add(v);
            }
        }

        if (!distinct.isEmpty()) {
            String combined = String.join(", ", distinct);
            field.getSetter().accept(profile, combined);
            confidenceScores.put(field.getPropertyName(), highestConfidence(items));
        }
    }

    private String resolveSingleValue(List<ExtractionResult> items,
                                      List<ConflictRecord> conflicts,
                                      BiodataField field,
                                      ExtractionContext context) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        // Collect distinct normalized values
        Map<String, List<ExtractionResult>> valueGroups = new LinkedHashMap<>();
        for (ExtractionResult item : items) {
            String val = item.getValue().trim();
            if (!val.isBlank()) {
                // Group case-insensitively for comparison
                String normKey = val.toLowerCase();
                valueGroups.computeIfAbsent(normKey, k -> new ArrayList<>()).add(item);
            }
        }

        if (valueGroups.isEmpty()) {
            return null;
        }

        if (valueGroups.size() == 1) {
            // Unanimous agreement across all evidence
            return valueGroups.values().iterator().next().get(0).getValue().trim();
        }

        // Multiple distinct values -> Genuine Contradiction!
        // 1. Pick best primary resolution (highest confidence, then longest informative string)
        ExtractionResult bestItem = selectBestEvidence(items);
        String primaryValue = bestItem.getValue().trim();

        // 2. Record the conflict
        List<String> competingValues = new ArrayList<>();
        for (List<ExtractionResult> group : valueGroups.values()) {
            competingValues.add(group.get(0).getValue().trim());
        }

        EvidenceKey key = new EvidenceKey(context, field);
        conflicts.add(ConflictRecord.builder()
                .key(key)
                .resolvedValue(primaryValue)
                .competingValues(competingValues)
                .competingEvidence(new ArrayList<>(items))
                .build());

        return primaryValue;
    }

    private ExtractionResult selectBestEvidence(List<ExtractionResult> items) {
        return items.stream().max(Comparator
                .comparingInt((ExtractionResult e) -> confidenceRank(e.getConfidence()))
                .thenComparingInt(e -> e.getValue().length())
        ).orElse(items.get(0));
    }

    private int confidenceRank(FieldConfidence conf) {
        if (conf == null) return 0;
        return switch (conf) {
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case CONFLICT -> 1;
            case MISSING -> 0;
        };
    }

    private FieldConfidence highestConfidence(List<ExtractionResult>... itemLists) {
        FieldConfidence best = FieldConfidence.LOW;
        for (List<ExtractionResult> list : itemLists) {
            if (list == null) continue;
            for (ExtractionResult item : list) {
                if (item.getConfidence() == FieldConfidence.HIGH) {
                    return FieldConfidence.HIGH;
                }
                if (item.getConfidence() == FieldConfidence.MEDIUM) {
                    best = FieldConfidence.MEDIUM;
                }
            }
        }
        return best;
    }

    private boolean hasConflictForField(List<ConflictRecord> conflicts, BiodataField field, ExtractionContext context) {
        for (ConflictRecord cr : conflicts) {
            if (cr.getKey().field() == field && cr.getKey().context() == context) {
                return true;
            }
        }
        return false;
    }

    private List<ExtractionResult> getEvidence(Map<EvidenceKey, List<ExtractionResult>> trail,
                                               BiodataField field,
                                               ExtractionContext... contexts) {
        List<ExtractionResult> result = new ArrayList<>();
        for (ExtractionContext ctx : contexts) {
            EvidenceKey key = new EvidenceKey(ctx, field);
            List<ExtractionResult> items = trail.get(key);
            if (items != null) {
                result.addAll(items);
            }
        }
        return result;
    }

    private void finalizeConfidenceScores(ProfileBiodata profile, Map<String, FieldConfidence> scores) {
        for (BiodataField field : BiodataField.values()) {
            if (field == BiodataField.SURNAME) continue;
            String propName = field.getPropertyName();
            String val = field.getGetter().apply(profile);
            if (val == null || val.isBlank()) {
                scores.put(propName, FieldConfidence.MISSING);
            } else if (!scores.containsKey(propName)) {
                scores.put(propName, FieldConfidence.HIGH);
            }
        }
    }

    private void markAllMissing(Map<String, FieldConfidence> scores) {
        for (BiodataField field : BiodataField.values()) {
            if (field != BiodataField.SURNAME) {
                scores.put(field.getPropertyName(), FieldConfidence.MISSING);
            }
        }
    }
}

