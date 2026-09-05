package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRequest {

    @NotBlank(message = "Biodata text is required.")
    @Size(max = 50_000, message = "Input exceeds maximum supported size (50000 characters). Please provide a smaller biodata.")
    private String rawText;

    private Boolean forceAi;
}
