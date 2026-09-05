package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates a discovered custom / non-canonical attribute along with its
 * extraction provenance and confidence score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAttribute {
    private String key;
    private String value;
    private FieldConfidence confidence;
    private ExtractionMethod method;
    private String sourceText;
}
