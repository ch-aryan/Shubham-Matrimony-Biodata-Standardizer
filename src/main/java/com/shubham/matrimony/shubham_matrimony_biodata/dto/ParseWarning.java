package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A single warning produced during parse result analysis.
 *
 * <p>
 * Each warning has a {@link WarningCategory}, a human-readable message,
 * and an optional list of details (e.g. the specific unrecognized lines).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseWarning {

    private WarningCategory category;

    private String message;

    @Builder.Default
    private List<String> details = new ArrayList<>();
}
