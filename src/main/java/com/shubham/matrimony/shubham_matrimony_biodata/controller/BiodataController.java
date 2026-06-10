package com.shubham.matrimony.shubham_matrimony_biodata.controller;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseRequest;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataServiceParser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biodata")
public class BiodataController {

    private final BiodataServiceParser biodataService;


    public BiodataController(BiodataServiceParser biodataService) {
        this.biodataService = biodataService;
    }

    @PostMapping("/parse")
    public ProfileBiodata parse(
            @RequestBody ParseRequest request) {

        return biodataService.parse(
                request.getRawText()
        );
    }
}