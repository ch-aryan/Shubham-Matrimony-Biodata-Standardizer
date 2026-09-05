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
                        BiodataLabels.FULL_NAME, 1),

        // Additional Non-Canonical Information fields
        WEIGHT("weight", (p, v) -> p.getAdditionalInfo().setWeight(v), p -> p.getAdditionalInfo().getWeight(),
                        Set.of(), 5),
        COMPLEXION("complexion", (p, v) -> p.getAdditionalInfo().setComplexion(v),
                        p -> p.getAdditionalInfo().getComplexion(),
                        Set.of(), 5),
        MARITAL_STATUS("maritalStatus", (p, v) -> p.getAdditionalInfo().setMaritalStatus(v),
                        p -> p.getAdditionalInfo().getMaritalStatus(),
                        Set.of(), 5),
        VISA_STATUS("visaStatus", (p, v) -> p.getAdditionalInfo().setVisaStatus(v),
                        p -> p.getAdditionalInfo().getVisaStatus(),
                        Set.of(), 5),
        RELIGION("religion", (p, v) -> p.getAdditionalInfo().setReligion(v), p -> p.getAdditionalInfo().getReligion(),
                        Set.of(), 5),
        MOTHER_TONGUE("motherTongue", (p, v) -> p.getAdditionalInfo().setMotherTongue(v),
                        p -> p.getAdditionalInfo().getMotherTongue(),
                        Set.of(), 5),
        RESIDENCE("residence", (p, v) -> p.getAdditionalInfo().setResidence(v),
                        p -> p.getAdditionalInfo().getResidence(),
                        Set.of(), 5),
        COUNTRY("country", (p, v) -> p.getAdditionalInfo().setCountry(v), p -> p.getAdditionalInfo().getCountry(),
                        Set.of(), 5),
        HOBBIES("hobbies", (p, v) -> p.getAdditionalInfo().setHobbies(v), p -> p.getAdditionalInfo().getHobbies(),
                        Set.of(), 5),
        PARTNER_PREFERENCES("partnerPreferences", (p, v) -> p.getAdditionalInfo().setPartnerPreferences(v),
                        p -> p.getAdditionalInfo().getPartnerPreferences(),
                        Set.of(), 5),
        PROPERTIES("properties", (p, v) -> {
                if (!p.getAdditionalInfo().getProperties().contains(v)) {
                        p.getAdditionalInfo().getProperties().add(v);
                }
        }, p -> !p.getAdditionalInfo().getProperties().isEmpty()
                        ? String.join(", ", p.getAdditionalInfo().getProperties())
                        : null,
                        Set.of(), 5),
        GRANDPARENTS("grandparents", (p, v) -> {
                if (!p.getAdditionalInfo().getPaternalGrandparents().contains(v)) {
                        p.getAdditionalInfo().getPaternalGrandparents().add(v);
                }
        }, p -> !p.getAdditionalInfo().getPaternalGrandparents().isEmpty()
                        ? String.join(", ", p.getAdditionalInfo().getPaternalGrandparents())
                        : null,
                        Set.of(), 5),
        CUSTOM_ATTRIBUTE("customAttributes", (p, v) -> {
        }, p -> null,
                        Set.of(), 1);

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

        public boolean isCanonical() {
                return this != SURNAME && this != CUSTOM_ATTRIBUTE
                                && this != WEIGHT && this != COMPLEXION && this != MARITAL_STATUS
                                && this != VISA_STATUS && this != RELIGION && this != MOTHER_TONGUE
                                && this != RESIDENCE && this != COUNTRY && this != HOBBIES
                                && this != PARTNER_PREFERENCES && this != PROPERTIES && this != GRANDPARENTS;
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