package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Corpus2DeterministicTest {

    private BiodataParserImplementation parser;

    @BeforeEach
    public void setUp() {
        parser = new BiodataParserImplementation();
    }

    @Test
    public void testBiodata24_StylizedFontsAndEmojis() {
        String input = """
            🌴గౌడ్స్ అమ్మాయి 🌴
            👧𝐍𝐀𝐌𝐄 :- 𝐌𝐚𝐲𝐮𝐫𝐢
            🗓️ 𝐃𝐎𝐁 :- 𝟏𝟏-𝟗-𝟏𝟗𝟗𝟓
            ⏰ 𝐓𝐎𝐁,:- 𝟗:𝟑𝟎 𝐀𝐌
            🗼 𝐇𝐞𝐢𝐠𝐡𝐭 :- 𝟓'𝟒
            🛎️ 𝐑𝐚𝐬𝐡𝐢 :- 𝐒𝐢𝐦𝐡𝐚🦁
            📘 𝐄𝐝𝐮𝐜𝐚𝐭𝐢𝐨𝐧 :- 𝐁. 𝐓𝐞𝐜𝐡
            👉𝐏𝐫𝐞𝐬𝐞𝐧𝐭 :- 𝐍𝐨𝐭 𝐖𝐨𝐫𝐤𝐢𝐧𝐠
            ⚓𝐏𝐫𝐞𝐩𝐚𝐫𝐢𝐧𝐠 𝐁𝐚𝐧𝐤 𝐉𝐨𝐛'𝐬 & 𝐌𝐞𝐝𝐢𝐜𝐚𝐥 𝐜𝐨𝐝𝐢𝐧𝐠
            👪👪👪👪👪
            🧖♂️𝐅𝐚𝐭𝐡𝐞𝐫 :- 𝐑𝐭𝐝 𝐁𝐮𝐬𝐢𝐧𝐞𝐬𝐬
            🧖♀️𝐌𝐨𝐭𝐡𝐞𝐫 :- 𝐇𝐨𝐦𝐞 𝐌𝐚𝐤𝐞𝐫
            👭𝐒𝐢𝐛𝐥𝐢𝐧𝐠'𝐬 :- 𝟏 𝐄𝐥𝐝𝐞𝐫 𝐒𝐢𝐬𝐭𝐞𝐫 𝐌𝐚𝐫𝐫𝐢𝐞𝐝 𝟐 𝐘𝐨𝐮𝐧𝐠𝐞𝐫 𝐁𝐫𝐨𝐭𝐡𝐞𝐫'𝐬
            🏠𝐃𝐢𝐬𝐭 :- 𝐉𝐚𝐧𝐚𝐠𝐨𝐧
            𝐑𝐞𝐪𝐮𝐢𝐫𝐞𝐦𝐞𝐧𝐭𝐬 :- 𝐎𝐧𝐥𝐲 𝐒𝐨𝐟𝐭𝐰𝐚𝐫𝐞 𝐄𝐧𝐠𝐢𝐧𝐞𝐞𝐫.
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Mayuri", profile.getFullName());
        assertEquals("11-9-1995", profile.getDateOfBirth());
        assertEquals("9:30 AM", profile.getTimeOfBirth());
        assertEquals("5'4", profile.getHeight());
        assertEquals("Simha", profile.getRashi());
        assertEquals("B. Tech", profile.getQualification());
        assertEquals("Not Working", profile.getOccupation());
        assertEquals("Rtd Business", profile.getFatherOccupation());
        assertEquals("Home Maker", profile.getMotherOccupation());
        assertNotNull(profile.getSiblingsDetails());
        assertTrue(profile.getSiblingsDetails().contains("Elder Sister"));

        // Verify dynamic open overflow
        assertNotNull(profile.getAdditionalInfo());
        assertNotNull(profile.getAdditionalInfo().getCustomAttributes());
        assertTrue(profile.getAdditionalInfo().getCustomAttributes().containsKey("Requirements")
                || profile.getAdditionalInfo().getCustomAttributes().containsKey("Dist"));

        // Verify evidence trail was generated
        assertNotNull(result.getEvidenceTrail());
        assertFalse(result.getEvidenceTrail().isEmpty());
    }

    @Test
    public void testBiodata19_RichAdditionalInfoAndDiet() {
        String input = """
            Keerthana Cherukuthota
            Date of Birth: 11-04-1999
            Place of Birth: Pune
            Time of Birth: 8:55 PM
            Height: 5’4”
            Weight: 91 kg
            Complexion: Wheatish
            Raasi: Capricorn (Makara)
            Nakshatram: Dhanishta
            Gothram: Pasupuneti
            Caste: Munnuru Kapu
            Marital Status: Never Married
            Diet : Pure Vegetarian

            Educational and Professional Details:
            Qualification: B.Tech in Computer Science, B.Ed, Diploma in Fashion Designing
            Job: Senior Software Engineer
            Company Name: ValueMomentum
            Salary/Package: 8.5 LPA
            Current Location: Hyderabad
            Email ID: cv.ramana3699@gmail.com
            Hobbies: Art, Stitching Outfits for the temple deities

            Family Background:
            Father’s Name: Sri Venkataramana
            Father’s Occupation: Business
            Father’s Native Place: Hyderabad
            Mother’s Name: Smt. Umarani
            Mother’s Occupation: Business
            Mother’s Parent’s Surname: Dadishetty
            Mother’s Native Place: Warangal
            Parents’ Residence/Location: Secunderabad
            Property/Assets: Well-settled family (₹9 Cr)

            Grandparents’ Details:
            Paternal Grandparents: Late Sri Cherukuthota Jayaram & Smt. Vijaya Lakshmi
            Maternal Grandparents: Late Sri Dadishetty Surya Prakash Rao & Smt. Vinoda

            Siblings & Their Marital Status:
            Younger Brother 1 Name: Ganesha
            Profession: Chartered Accountant, CFA
            Location: Hyderabad
            Marital Status: Unmarried

            Younger Brother 2 Name: Gurumurthy
            Profession: Advocate
            Location: Pune
            Marital Status: Unmarried

            Partner Preferences:
            Age: Between 28 - 32 years
            Height: 5’5” & above
            Education/Occupation: Any graduation
            Location: Any
            Property/Income Expectations: Well-settled family

            Family Contact Number: 9542793699
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Keerthana Cherukuthota", profile.getFullName());
        assertEquals("11-04-1999", profile.getDateOfBirth());
        assertEquals("8:55 PM", profile.getTimeOfBirth());
        assertEquals("Pune", profile.getPlaceOfBirth());
        assertEquals("5’4”", profile.getHeight());
        assertEquals("Munnuru Kapu", profile.getCaste());
        assertEquals("Pasupuneti", profile.getGothram());
        assertTrue(profile.getQualification().contains("B.Tech in Computer Science"));
        assertEquals("Senior Software Engineer", profile.getOccupation());
        assertEquals("ValueMomentum", profile.getCompany());
        assertEquals("8.5 LPA", profile.getSalary());
        assertEquals("Hyderabad", profile.getCurrentLocation());

        assertEquals("Sri Venkataramana", profile.getFatherName());
        assertEquals("Business", profile.getFatherOccupation());
        assertEquals("Smt. Umarani", profile.getMotherName());
        assertEquals("Business", profile.getMotherOccupation());

        // Verify structured AdditionalInformation
        assertNotNull(profile.getAdditionalInfo());
        assertEquals("Wheatish", profile.getAdditionalInfo().getComplexion());
        assertEquals("91 kg", profile.getAdditionalInfo().getWeight());
        assertEquals("Never Married", profile.getAdditionalInfo().getMaritalStatus());
        assertFalse(profile.getAdditionalInfo().getProperties().isEmpty());
        assertFalse(profile.getAdditionalInfo().getPaternalGrandparents().isEmpty());
        assertFalse(profile.getAdditionalInfo().getMaternalGrandparents().isEmpty());
        assertFalse(profile.getAdditionalInfo().getPartnerPreferences().isEmpty());

        // Verify Dynamic Open Overflow captured Diet without requiring schema column
        assertEquals("Pure Vegetarian", profile.getAdditionalInfo().getCustomAttributes().get("Diet"));
    }

    @Test
    public void testBiodata17_PalapalliAnirudh() {
        String input = """
            Name: Palapalli Anirudh
            Date of Birth: 07/04/2001
            Time of Birth: 2:00 AM
            Place of Birth: Hyderabad
            Nakshatram: Uttarphalguni (3rd pada)
            Rashi: Kanya
            Height: 5.7
            Education: MBA
            Profession: Senior Accountant
            Organization: Nine Advisory
            Annual Income: 7.2 Lakhs

            Father Name: Palapalli Eshwar Rao
            Mother Name: Palapalli Supriya
            """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Palapalli Anirudh", profile.getFullName());
        assertEquals("07/04/2001", profile.getDateOfBirth());
        assertEquals("2:00 AM", profile.getTimeOfBirth());
        assertEquals("Hyderabad", profile.getPlaceOfBirth());
        assertEquals("Uttarphalguni (3rd pada)", profile.getNakshatram());
        assertEquals("Kanya", profile.getRashi());
        assertEquals("5.7", profile.getHeight());
        assertEquals("MBA", profile.getQualification());
        assertEquals("Senior Accountant", profile.getOccupation());
        assertEquals("Nine Advisory", profile.getCompany());
        assertEquals("7.2 Lakhs", profile.getSalary());
        assertEquals("Palapalli Eshwar Rao", profile.getFatherName());
        assertEquals("Palapalli Supriya", profile.getMotherName());

        assertFalse(result.hasConflicts());
    }
}
