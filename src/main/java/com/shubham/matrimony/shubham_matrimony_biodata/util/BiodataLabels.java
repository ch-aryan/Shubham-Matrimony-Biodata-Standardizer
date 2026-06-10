package com.shubham.matrimony.shubham_matrimony_biodata.util;

import java.util.Set;

public class BiodataLabels {

    private BiodataLabels() {
    }

    public static final Set<String> FULL_NAME = Set.of(
            "name",
            "full name",
            "surname",
            "middle name",
            "bride name",
            "groom name",
            "abbai peru",
            "ammai peru",
            "peru",
            "inti peru",
            "enti peru"
    );

    public static final Set<String> DATE_OF_BIRTH = Set.of(
            "date of birth",
            "putinadinam",
            "dob",
            "birth date"
    );

    public static final Set<String> TIME_OF_BIRTH = Set.of(
            "time of birth",
            "tob",
            "time",
            "birth time"
    );

    public static final Set<String> PLACE_OF_BIRTH = Set.of(
            "place of birth",
            "birth place"
    );

    public static final Set<String> OCCUPATION = Set.of(
            "occupation",
            "profession",
            "job",
            "working as"
    );

    public static final Set<String> QUALIFICATION = Set.of(
            "qualification",
            "education",
            "highest education"
    );

    public static final Set<String> HEIGHT = Set.of(
            "height"
    );

    public static final Set<String> CURRENT_LOCATION = Set.of(
            "current location",
            "native place",
            "living in",
            "settled in",
            "staying in",
            "location",
            "present location"
    );

    public static final Set<String> FATHER_NAME = Set.of(
            "father name",
            "father's name"
    );

    public static final Set<String> MOTHER_NAME = Set.of(
            "mother name",
            "mother's name"
    );
}