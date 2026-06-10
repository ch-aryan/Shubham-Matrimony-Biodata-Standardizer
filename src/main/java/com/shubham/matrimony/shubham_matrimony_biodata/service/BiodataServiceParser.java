package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;

public interface BiodataServiceParser {
    ProfileBiodata parse(String rawText);
}
