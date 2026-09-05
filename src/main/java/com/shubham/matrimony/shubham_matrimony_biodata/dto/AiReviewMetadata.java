package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Operational metadata providing transparency on whether and how AI semantic review
 * was executed for a parsing request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewMetadata {

    private boolean invoked;
    private String provider;
    private String model;
    private long latencyMs;
    private String routingReason;

    @Builder.Default
    private List<String> fieldsCompleted = new ArrayList<>();

    @Builder.Default
    private List<String> fieldsCorrected = new ArrayList<>();

    @Builder.Default
    private List<String> conflictsReviewed = new ArrayList<>();

    private String errorMessage;
}

