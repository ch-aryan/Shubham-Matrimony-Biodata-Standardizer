package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RealBiodataSamplesTest {

    private BiodataParserImplementation parser;

    @BeforeEach
    public void setUp() {
        parser = new BiodataParserImplementation();
    }

    @Test
    public void testSample1_BhavaniManne() {
        String input = """
            Name: Bhavani Manne 
            DOB : 01-DEC-1996 
            Time: 01:30 PM
            Height : 5.4 
            Rashi: Karkataka 
            Star: Ashlesha 
            Education: Btech(CSE)
            Job Details: Software Engineer,Cigniti Technology, Hitechcity 
            Place of Birth: Hyderabad 
            Family Details Father: Manne Srinivas(Business) 
            Mother: Manne Prabhavathi(House Wife) 
            Siblings: Elder Brother(Business).
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Bhavani Manne", profile.getFullName());
        assertEquals("01-DEC-1996", profile.getDateOfBirth());
        assertEquals("01:30 PM", profile.getTimeOfBirth());
        assertEquals("5.4", profile.getHeight());
        assertEquals("Karkataka", profile.getRashi());
        assertEquals("Ashlesha", profile.getNakshatram());
        assertEquals("Btech(CSE)", profile.getQualification());
        assertEquals("Hyderabad", profile.getPlaceOfBirth());
        assertEquals("Manne Srinivas", profile.getFatherName());
        assertEquals("Business", profile.getFatherOccupation());
        assertEquals("Manne Prabhavathi", profile.getMotherName());
        assertEquals("House Wife", profile.getMotherOccupation());
        assertNotNull(profile.getSiblingsDetails());
        assertTrue(profile.getSiblingsDetails().contains("Elder Brother"));
    }

    @Test
    public void testSample2_RamshettyVignesh() {
        String input = """
            Name: Ramshetty Vignesh Date of birth: 19-11-1996 Timings: 09:06 PM Rashi:Kumba Height : 5.8
            Caste: Munurukapu
            Gothram: Paspuneti 
            Company Name: Salesforce Designation: Software Engineer 
            Salary: 18 Lakhs Per Annum 
            Father Name: Ramshetty Jagadishwar
            Occupation: RMP Doctor 
            Mother Name: Ramshetty Navaneetha
            Occupation: House Wife
            Sister Name: Ramshetty Vaishnavi - working as a software engineer at Infor Adress: Shamshabad , Beside registeration office Properties : Own house G+1 in Shamshabad - 
            rental income - 25k per month 1.200 sq yards plot in Shamshabad 2.250 sq yards plot in balnagar
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Ramshetty Vignesh", profile.getFullName());
        assertEquals("19-11-1996", profile.getDateOfBirth());
        assertEquals("09:06 PM", profile.getTimeOfBirth());
        assertEquals("Kumba", profile.getRashi());
        assertEquals("5.8", profile.getHeight());
        assertEquals("Munurukapu", profile.getCaste());
        assertEquals("Paspuneti", profile.getGothram());
        assertEquals("Salesforce", profile.getCompany());
        assertEquals("Software Engineer", profile.getOccupation());
        assertEquals("18 Lakhs Per Annum", profile.getSalary());
        assertEquals("Ramshetty Jagadishwar", profile.getFatherName());
        assertEquals("RMP Doctor", profile.getFatherOccupation());
        assertEquals("Ramshetty Navaneetha", profile.getMotherName());
        assertEquals("House Wife", profile.getMotherOccupation());
    }

    @Test
    public void testSample3_KashaTejaswini() {
        String input = """
            Name:- Kasha Tejaswini 
            Father name:- Kasha Ramesh( Real-estate)
            Mother name:- Kasha Saritha(House wife) 
            DOB:- 12-10-1999 
            Highest Qualification:- B.com 
            Current organisation:- Sitel 
            Birth time:- 8:30 Am - 9:00Am 
            Rashi:- Thula rashi 
            Nakhsathram:- Vishakha 
            Height :- 5.6 Siblings - 1 younger sister
            Name- kasha Bhavani - studying..
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Kasha Tejaswini", profile.getFullName());
        assertEquals("Kasha Ramesh", profile.getFatherName());
        assertEquals("Real-estate", profile.getFatherOccupation());
        assertEquals("Kasha Saritha", profile.getMotherName());
        assertEquals("House wife", profile.getMotherOccupation());
        assertEquals("12-10-1999", profile.getDateOfBirth());
        assertEquals("B.com", profile.getQualification());
        assertEquals("Sitel", profile.getCompany());
        assertEquals("8:30 Am - 9:00Am", profile.getTimeOfBirth());
        assertEquals("Thula", profile.getRashi());
        assertEquals("Vishakha", profile.getNakshatram());
        assertEquals("5.6", profile.getHeight());
    }
}
