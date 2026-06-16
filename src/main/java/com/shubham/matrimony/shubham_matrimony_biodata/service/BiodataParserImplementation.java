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
                String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            if (matchesLabel(line, BiodataLabels.FULL_NAME)) {
                profile.setFullName(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.DATE_OF_BIRTH)) {
                profile.setDateOfBirth(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.TIME_OF_BIRTH)) {
                profile.setTimeOfBirth(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.PLACE_OF_BIRTH)) {
                profile.setPlaceOfBirth(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.CURRENT_LOCATION)) {
                profile.setCurrentLocation(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.HEIGHT)) {
                profile.setHeight(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.QUALIFICATION)) {
                profile.setQualification(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.OCCUPATION)) {
                profile.setOccupation(extractValue(line));
            }
            else if(matchesLabel(line, BiodataLabels.CASTE)) {
                profile.setOccupation(extractValue(line));
            }
            else if(matchesLabel(line, BiodataLabels.SALARY)) {
                profile.setOccupation(extractValue(line));
            }
            else if(matchesLabel(line, BiodataLabels.COMPANY)) {
                profile.setOccupation(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.GOTHRAM)) {
                profile.setGothram(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.RASHI)) {
                profile.setRashi(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.NAKSHATRAM)) {
                profile.setNakshatram(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.FATHER_NAME)) {
                profile.setFatherName(extractValue(line));
            }
            else if (matchesLabel(line, BiodataLabels.MOTHER_NAME)) {
                profile.setMotherName(extractValue(line));
            }
        }
                return profile;
            }
        }

/*
One improvement I'd make now

Instead of writing 20+ else if blocks, create a helper method:

private void setIfMatched(
        String line,
        Set<String> labels,
        Consumer<String> setter) {

    if (matchesLabel(line, labels)) {
        setter.accept(extractValue(line));
    }
}

Then your loop becomes:

for (String line : lines) {

    setIfMatched(
            line,
            BiodataLabels.FULL_NAME,
            profile::setFullName
    );

    setIfMatched(
            line,
            BiodataLabels.DATE_OF_BIRTH,
            profile::setDateOfBirth
    );

    setIfMatched(
            line,
            BiodataLabels.OCCUPATION,
            profile::setOccupation
    );
}

Don't do this immediately if you're still learning Spring and Java fundamentals. The long if-else version is perfectly fine for now and easier to debug.

My recommendation for the next commit:

feat: parse additional biodata fields

- Added DOB parsing
- Added TOB parsing
- Added birth place parsing
- Added qualification parsing
- Added location parsing
- Added father and mother details parsing
- Expanded biodata label dictionary

That will take your project from a proof-of-concept parser to a genuinely useful biodata extraction engine.
 */