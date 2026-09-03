package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;

public interface BiodataServiceParser {
    ProfileBiodata parse(String rawText);

    ExtractionResultDTO parseBiodata(String rawText);

    ParseResponse parseAndValidate(String rawText);
}
