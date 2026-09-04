package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import org.junit.jupiter.api.Test;

public class SatwikDiagnosticTest {

    @Test
    public void diagnoseSatwik() {
        String input = """
                Satwik Kotte
                Date of Birth: 25-07-1997
                Place of Birth: Godavarikhani
                Time of Birth: 2:55 AM
                Height: 5’6”
                Weight: 65 Kgs
                Complexion: Fair
                Raasi: Meena
                Nakshatram: Uthara Bhadra
                Gothram: Pasunooti

                Caste: Munnuru Kapu
                Marital Status: Never Married

                Educational and Professional Details:
                Qualification: B.Tech in Electronics and Computer Engineering, Sreenidhi Engineering College, Hyderabad
                Profession: Software Engineer
                Company Name: Infinite
                Salary / Package: ₹15 Lakhs per annum
                Current Location: Hyderabad
                Hobbies: Reading Books, Playing Cricket

                Family Background:
                Father’s Name: Sri Kotte Srisailam
                Father’s Occupation: Private School (Own School) Correspondent, Real Estate Business, Journalist
                Father’s Native Place: Garepally (Village), Kataram (Mandal), Jayashanker Bhupalapally District
                Mother’s Name: Smt. Pusphalatha
                Mother’s Occupation: Government Employee, Education Department
                Mother’s Parents’ Surname: Kayitha
                Mother’s Native Place: Damerakunta, Jayashanker Bhupalapally District
                Parents’ Residence/Location: Kataram Garepally, Bhupalapally
                Property / Assets Details: Well-settled family

                Grandparents Details:
                Paternal Grandparents: Sri Kotte Chandraiah & Smt. Kotte Buchamma
                Maternal Grandparents: Sri Kayitha Sambaiah & Smt. Kayitha Amrutha

                Sibling & Marital Status:
                Sibling: Elder Sister
                Name: Kotte Sahithi
                Profession: Software Engineer (Cognizant)
                Location: Hyderabad
                Marital Status: Married
                Spouse Name: Akula Vinayak
                Spouse Profession: IT Audit Manager
                Spouse Family Details: Akula Ramkrishna & Padmavathi
                """;

        BiodataParserImplementation parser = new BiodataParserImplementation();
        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata p = result.getProfile();

        org.junit.jupiter.api.Assertions.assertEquals("Satwik Kotte", p.getFullName());
        org.junit.jupiter.api.Assertions.assertEquals("25-07-1997", p.getDateOfBirth());
        org.junit.jupiter.api.Assertions.assertEquals("2:55 AM", p.getTimeOfBirth());
        org.junit.jupiter.api.Assertions.assertEquals("Godavarikhani", p.getPlaceOfBirth());
        org.junit.jupiter.api.Assertions.assertEquals("Munnuru Kapu", p.getCaste());
        org.junit.jupiter.api.Assertions.assertEquals("Pasunooti", p.getGothram());
        org.junit.jupiter.api.Assertions.assertEquals("Meena", p.getRashi());
        org.junit.jupiter.api.Assertions.assertEquals("Uthara Bhadra", p.getNakshatram());
        org.junit.jupiter.api.Assertions.assertEquals("Software Engineer", p.getOccupation());
        org.junit.jupiter.api.Assertions.assertEquals("Infinite", p.getCompany());
        org.junit.jupiter.api.Assertions.assertEquals("Hyderabad", p.getCurrentLocation());
        org.junit.jupiter.api.Assertions.assertEquals("Sri Kotte Srisailam", p.getFatherName());
        org.junit.jupiter.api.Assertions.assertEquals("Smt. Pusphalatha", p.getMotherName());
        org.junit.jupiter.api.Assertions.assertEquals("Never Married", p.getAdditionalInfo().getMaritalStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Elder Sister: Kotte Sahithi (Software Engineer (Cognizant))", p.getSiblingsDetails());
        org.junit.jupiter.api.Assertions.assertTrue(p.getAdditionalInfo().getProperties().contains("Well-settled family"));
        org.junit.jupiter.api.Assertions.assertFalse(p.getFullName().contains("Vinayak"));
        org.junit.jupiter.api.Assertions.assertFalse(p.getSiblingsDetails().contains("Vinayak"));
        org.junit.jupiter.api.Assertions.assertFalse(p.getSiblingsDetails().contains("Status"));
    }
}
