package com.shubham.matrimony.shubham_matrimony_biodata.dto;

import lombok.Data;

@Data
public class ProfileBiodata {

    // Personal Details
    private String fullName;
    private String dateOfBirth;
    private String timeOfBirth;
    private String placeOfBirth;

    private String currentLocation;
    private String nativePlace;

    private String height;

    // Horoscope Details
    private String caste;
    private String gothram;
    private String rashi;
    private String nakshatram;

    // Education & Career
    private String qualification;

    private String occupation;
    private String company;

    private String salary;

    // Family Details
    private String fatherName;
    private String fatherOccupation;

    private String motherName;
    private String motherOccupation;

    private String siblingsDetails;
}