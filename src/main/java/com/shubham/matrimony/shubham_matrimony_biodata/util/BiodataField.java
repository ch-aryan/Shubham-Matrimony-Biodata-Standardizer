package com.shubham.matrimony.shubham_matrimony_biodata.util;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum BiodataField {

    // Priority 20: Evaluated BEFORE candidate OCCUPATION & generic NAME
    FATHER_OCCUPATION("fatherOccupation", ProfileBiodata::setFatherOccupation, ProfileBiodata::getFatherOccupation,
            BiodataLabels.FATHER_OCCUPATION, 20),
    MOTHER_OCCUPATION("motherOccupation", ProfileBiodata::setMotherOccupation, ProfileBiodata::getMotherOccupation,
            BiodataLabels.MOTHER_OCCUPATION, 20),
    FATHER_NAME("fatherName", ProfileBiodata::setFatherName, ProfileBiodata::getFatherName,
            BiodataLabels.FATHER_NAME, 20),
    MOTHER_NAME("motherName", ProfileBiodata::setMotherName, ProfileBiodata::getMotherName,
            BiodataLabels.MOTHER_NAME, 20),
    SIBLINGS("siblingsDetails", ProfileBiodata::setSiblingsDetails, ProfileBiodata::getSiblingsDetails,
            BiodataLabels.SIBLINGS, 15),

    // Priority 10: Specific canonical attributes
    DATE_OF_BIRTH("dateOfBirth", ProfileBiodata::setDateOfBirth, ProfileBiodata::getDateOfBirth,
            BiodataLabels.DATE_OF_BIRTH, 10),
    TIME_OF_BIRTH("timeOfBirth", ProfileBiodata::setTimeOfBirth, ProfileBiodata::getTimeOfBirth,
            BiodataLabels.TIME_OF_BIRTH, 10),
    PLACE_OF_BIRTH("placeOfBirth", ProfileBiodata::setPlaceOfBirth, ProfileBiodata::getPlaceOfBirth,
            BiodataLabels.PLACE_OF_BIRTH, 10),
    CASTE("caste", ProfileBiodata::setCaste, ProfileBiodata::getCaste,
            BiodataLabels.CASTE, 10),
    GOTHRAM("gothram", ProfileBiodata::setGothram, ProfileBiodata::getGothram,
            BiodataLabels.GOTHRAM, 10),
    RASHI("rashi", ProfileBiodata::setRashi, ProfileBiodata::getRashi,
            BiodataLabels.RASHI, 10),
    NAKSHATRAM("nakshatram", ProfileBiodata::setNakshatram, ProfileBiodata::getNakshatram,
            BiodataLabels.NAKSHATRAM, 10),
    QUALIFICATION("qualification", (profile, value) -> {
        if (profile.getQualification() == null || profile.getQualification().isBlank()) {
            profile.setQualification(value);
        } else if (!profile.getQualification().contains(value)) {
            profile.setQualification(profile.getQualification() + " (" + value + ")");
        }
    }, ProfileBiodata::getQualification, BiodataLabels.QUALIFICATION, 10),
    SALARY("salary", ProfileBiodata::setSalary, ProfileBiodata::getSalary,
            BiodataLabels.SALARY, 10),
    COMPANY("company", ProfileBiodata::setCompany, ProfileBiodata::getCompany,
            BiodataLabels.COMPANY, 10),
    CURRENT_LOCATION("currentLocation", ProfileBiodata::setCurrentLocation, ProfileBiodata::getCurrentLocation,
            BiodataLabels.CURRENT_LOCATION, 10),
    NATIVE_PLACE("nativePlace", ProfileBiodata::setNativePlace, ProfileBiodata::getNativePlace,
            BiodataLabels.NATIVE_PLACE, 10),
    HEIGHT("height", ProfileBiodata::setHeight, ProfileBiodata::getHeight,
            BiodataLabels.HEIGHT, 10),

    // Lower priority generic terms
    OCCUPATION("occupation", ProfileBiodata::setOccupation, ProfileBiodata::getOccupation,
            BiodataLabels.OCCUPATION, 5),
    SURNAME("surname", ProfileBiodata::setFullName, ProfileBiodata::getFullName,
            BiodataLabels.SURNAME, 3),
    FULL_NAME("fullName", ProfileBiodata::setFullName, ProfileBiodata::getFullName,
            BiodataLabels.FULL_NAME, 1);

    private final String propertyName;
    private final BiConsumer<ProfileBiodata, String> setter;
    private final Function<ProfileBiodata, String> getter;
    private final Set<String> aliases;
    private final int priority;

    BiodataField(String propertyName,
            BiConsumer<ProfileBiodata, String> setter,
            Function<ProfileBiodata, String> getter,
            Set<String> aliases,
            int priority) {
        this.propertyName = propertyName;
        this.setter = setter;
        this.getter = getter;
        this.aliases = aliases;
        this.priority = priority;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public BiConsumer<ProfileBiodata, String> getSetter() {
        return setter;
    }

    public Function<ProfileBiodata, String> getGetter() {
        return getter;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public int getPriority() {
        return priority;
    }

    public void apply(ProfileBiodata profile, String value) {
        if (value != null && !value.isBlank()) {
            setter.accept(profile, value.trim());
        }
    }

    public static List<BiodataField> allSortedByPriority() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(BiodataField::getPriority).reversed())
                .toList();
    }
}
/*
 * The Smart Enum:
 * BiodataField.java
 * In modern Java, an enum is not just a list of words — it is a full Java
 * class. It can store data, priority levels, and even executable functions.
 * 
 * java
 * 
 * 
 * public enum BiodataField {
 * FATHER_NAME(
 * ProfileBiodata::setFatherName, // Method Reference (Function to set the
 * value)
 * ProfileBiodata::getFatherName, // Method Reference (Function to read the
 * value)
 * BiodataLabels.FATHER_NAME, // Its allowed aliases ("father name",
 * "thandri peru", etc.)
 * 10 // High priority (check before generic 'Name')
 * ),
 * FULL_NAME(
 * ProfileBiodata::setFullName,
 * ProfileBiodata::getFullName,
 * BiodataLabels.FULL_NAME,
 * 1 // Lower priority
 * );
 * What is ProfileBiodata::setFullName?
 * This is a Java Method Reference (using BiConsumer<ProfileBiodata, String>).
 * Instead of writing an if-else to manually call .setFullName(), we pass the
 * function itself as a parameter!
 * 
 * When the parser finds a value, it simply calls:
 * 
 * java
 * 
 * 
 * segment.getField().apply(profile, "Manasa");
 * And Java automatically knows which setter on ProfileBiodata to invoke!
 * 
 * 3. How the Engine in
 * BiodataParserUtils.java
 * Works
 * Instead of treating text as just "one line = one field", the engine uses a
 * Segment Tokenizer (like a compiler/lexer):
 * 
 * Example Input:
 * text
 * 
 * 
 * "Name: Manasa | DOB: 02/11/1998 | Job: SDE @ Amazon"
 * Step 1: Rule Compilation (Pre-sorted by Length & Priority)
 * In the static block, we compile all aliases and sort them so longer phrases
 * are searched first:
 * 
 * "father's occupation" (length 19) is checked before "occupation" (length 10)
 * "father name" (length 11) is checked before "name" (length 4)
 * This guarantees that "Father Name: Ramesh" will never accidentally trigger
 * the rule for candidate "Name".
 * 
 * Step 2: Finding Label Positions (Offset Scanning)
 * The engine scans the string and finds where labels start and end:
 * 
 * 
 * 
 * "Name: Manasa | DOB: 02/11/1998 | Job: SDE @ Amazon"
 * ↑ ↑ ↑
 * [Label 1] [Label 2] [Label 3]
 * Pos: 0 to 4 Pos: 16 to 19 Pos: 34 to 37
 * Step 3: Slicing the Values Between Labels
 * Once the boundaries are known, the text between labels is extracted:
 * 
 * Label Found Raw Slice Extracted Cleaned Value Applied Field
 * Name : Manasa | Manasa fullName
 * DOB : 02/11/1998 | 02/11/1998 dateOfBirth
 * Job : SDE @ Amazon SDE @ Amazon occupation
 * Step 4: Value Sanitization (cleanValue)
 * Real WhatsApp data is messy:
 * 
 * 1. Name : Manasa
 * • Height = 5'4"
 * Company - Amazon,
 * cleanValue strips out leading bullets (*, •, -), colon/equal signs (:, =),
 * and trailing commas or pipes, while safely keeping digits for dates,
 * salaries, and height.
 * 
 * 4. Why this makes your Parser Code in
 * BiodataParserImplementation.java
 * so Simple
 * Now your main parser is just 10 lines of clean Java:
 * 
 * java
 * 
 * 
 * @Service
 * public class BiodataParserImplementation implements BiodataServiceParser {
 * 
 * @Override
 * public ProfileBiodata parse(String rawText) {
 * ProfileBiodata profile = new ProfileBiodata();
 * if (rawText == null || rawText.isBlank()) return profile;
 * String[] lines = rawText.split("\\r?\\n");
 * for (String line : lines) {
 * String stripped = BiodataParserUtils.stripLeadingBullets(line);
 * List<ParsedSegment> segments =
 * BiodataParserUtils.parseTextSegments(stripped);
 * 
 * for (ParsedSegment segment : segments) {
 * // Apply the extracted value to the right field automatically!
 * segment.getField().apply(profile, segment.getValue());
 * }
 * }
 * return profile;
 * }
 * }
 * Summary of Benefits
 * Zero duplicate if-else boilerplate.
 * Handles single-line multi-fields (|, ,, spaces, inline).
 * No keyword collisions (Father Name vs Name).
 * Instant extensibility: If you add a new field tomorrow (e.g. Rahu Kethu
 * Dosham), you just add one line to the enum, and the entire parser
 * automatically supports it without modifying any parsing logic.
 */