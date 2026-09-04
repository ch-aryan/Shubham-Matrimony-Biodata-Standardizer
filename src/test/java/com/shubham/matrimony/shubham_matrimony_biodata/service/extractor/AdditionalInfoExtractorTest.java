package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.AdditionalInformation;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataParserImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AdditionalInfoExtractorTest {

    private BiodataParserImplementation parser;

    @BeforeEach
    public void setUp() {
        parser = new BiodataParserImplementation();
    }

    @Test
    public void testPropertiesPreservation() {
        String input = """
                Name: Ramshetty Vignesh
                DOB: 19-11-1996
                Properties:
                Own house G+1 in Shamshabad - rental income - 25k per month
                1.200 sq yards plot in Shamshabad
                2.250 sq yards plot in balnagar
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        AdditionalInformation info = result.getProfile().getAdditionalInfo();

        assertNotNull(info);
        assertTrue(info.hasContent());
        assertEquals(3, info.getProperties().size());
        assertTrue(info.getProperties().get(0).contains("Own house G+1"));
        assertTrue(info.getProperties().get(1).contains("200 sq yards plot"));
        assertTrue(info.getProperties().get(2).contains("250 sq yards plot"));

        // Crucial principle: these property lines should NOT pollute unparsedLines
        assertFalse(result.getUnparsedLines().stream().anyMatch(l -> l.contains("Own house G+1")));
    }

    @Test
    public void testPhysicalTraitsAndHobbies() {
        String input = """
                Name: Satwik Kotte
                DOB: 25-07-1997
                Height: 5'6"
                Weight: 65 Kgs
                Complexion: Fair
                Marital Status: Never Married
                Hobbies: Reading Books, Playing Cricket
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        AdditionalInformation info = result.getProfile().getAdditionalInfo();

        assertNotNull(info);
        assertEquals("65 Kgs", info.getWeight());
        assertEquals("Fair", info.getComplexion());
        assertEquals("Never Married", info.getMaritalStatus());
        assertEquals("Reading Books, Playing Cricket", info.getHobbies());

        // Canonical core fields are still properly extracted
        assertEquals("Satwik Kotte", result.getProfile().getFullName());
        assertEquals("25-07-1997", result.getProfile().getDateOfBirth());
        assertEquals("5'6", result.getProfile().getHeight());
    }

    @Test
    public void testVisaAndResidence() {
        String input = """
                Name: Rohith Varala
                DOB: 02-02-1992
                Visa Status: Temporary Resident (TR)
                Religion: Hindu
                Mother Tongue: Telugu
                Residence: Raleigh, North Carolina
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        AdditionalInformation info = result.getProfile().getAdditionalInfo();

        assertNotNull(info);
        assertEquals("Temporary Resident (TR)", info.getVisaStatus());
        assertEquals("Hindu", info.getReligion());
        assertEquals("Telugu", info.getMotherTongue());
        assertEquals("Raleigh, North Carolina", info.getResidence());
    }

    @Test
    public void testGrandparentsPreservation() {
        String input = """
                Name: Sai Akshay
                DOB: 03-09-1996
                Paternal Grandparents: Sri Kotte Chandraiah & Smt. Kotte Buchamma
                Maternal Grandparents: Sri Kayitha Sambaiah & Smt. Kayitha Amrutha
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        AdditionalInformation info = result.getProfile().getAdditionalInfo();

        assertNotNull(info);
        assertEquals(1, info.getPaternalGrandparents().size());
        assertTrue(info.getPaternalGrandparents().get(0).contains("Sri Kotte Chandraiah"));
        assertEquals(1, info.getMaternalGrandparents().size());
        assertTrue(info.getMaternalGrandparents().get(0).contains("Sri Kayitha Sambaiah"));
    }
}
