package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates all non-canonical, extended matrimonial biodata information.
 *
 * <p>Preserves valuable domain details (properties, grandparents, international visas,
 * physical traits, lifestyle, hobbies) in a single structured object without polluting
 * the core 22-field canonical profile or requiring dozens of extra database table columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInformation {

    // ── Properties & Assets ──────────────────────────────────────────────────
    @Builder.Default
    private List<String> properties = new ArrayList<>();

    // ── Grandparents ─────────────────────────────────────────────────────────
    @Builder.Default
    private List<String> paternalGrandparents = new ArrayList<>();

    @Builder.Default
    private List<String> maternalGrandparents = new ArrayList<>();

    // ── Extended Family & In-laws ────────────────────────────────────────────
    @Builder.Default
    private List<String> extendedFamily = new ArrayList<>();

    // ── International & Residency ────────────────────────────────────────────
    private String visaStatus;
    private String country;
    private String residence;

    // ── Physical & Personal Attributes ───────────────────────────────────────
    private String complexion;
    private String weight;
    private String maritalStatus;
    private String religion;
    private String motherTongue;
    private String hobbies;

    // ── Partner Preferences ──────────────────────────────────────────────────
    private String partnerPreferences;

    // ── Custom / Arbitrary Key-Value Attributes ──────────────────────────────
    @Builder.Default
    private Map<String, String> customAttributes = new LinkedHashMap<>();

    // ── Unstructured Notes Preserved Without Loss ────────────────────────────
    @Builder.Default
    private List<String> rawNotes = new ArrayList<>();

    /**
     * Checks if this object contains any preserved information.
     *
     * @return {@code true} if at least one field or list is populated; {@code false} otherwise.
     */
    public boolean hasContent() {
        return !properties.isEmpty()
                || !paternalGrandparents.isEmpty()
                || !maternalGrandparents.isEmpty()
                || !extendedFamily.isEmpty()
                || visaStatus != null
                || country != null
                || residence != null
                || complexion != null
                || weight != null
                || maritalStatus != null
                || religion != null
                || motherTongue != null
                || hobbies != null
                || partnerPreferences != null
                || !customAttributes.isEmpty()
                || !rawNotes.isEmpty();
    }
}

