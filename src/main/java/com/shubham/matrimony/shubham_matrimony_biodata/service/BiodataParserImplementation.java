package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataLabels;
import org.springframework.stereotype.Service;

import static com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils.extractValue;
import static com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataParserUtils.matchesLabel;
@Service
public class BiodataParserImplementation implements BiodataServiceParser{

    @Override
    public ProfileBiodata parse(String rawText) {

        ProfileBiodata profile = new ProfileBiodata();

                String[] lines =
                        rawText.split("\\r?\\n");

                for(String line : lines){

                    if(matchesLabel(
                            line,
                            BiodataLabels.FULL_NAME)){

                        profile.setFullName(
                                extractValue(line)
                        );
                    }

                    else if(matchesLabel(
                            line,
                            BiodataLabels.DATE_OF_BIRTH)){

                        profile.setDateOfBirth(
                                extractValue(line)
                        );
                    }

                    else if(matchesLabel(
                            line,
                            BiodataLabels.OCCUPATION)){

                        profile.setOccupation(
                                extractValue(line)
                        );
                    }
                }

                return profile;
            }
        }

