package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.Data;

@Data
public class ParseRequest {
    private String rawText;
    private Boolean forceAi;
}
