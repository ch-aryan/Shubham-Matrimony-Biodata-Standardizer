package com.shubham.matrimony.shubham_matrimony_biodata.service;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResultDTO;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ProfileBiodata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiodataParserImplementationTest {

    private final BiodataParserImplementation parser =
            new BiodataParserImplementation();

    @Test
    void shouldParseBasicBiodata() {
        String rawText = """
                Name: Aryan
                Date of Birth: 10-10-1999
                Occupation: Developer
                """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Aryan", profile.getFullName());
        assertEquals("10-10-1999", profile.getDateOfBirth());
        assertEquals("Developer", profile.getOccupation());
    }

    @Test
    void shouldParseMultipleFields() {
        String rawText = """
            Name: Aryan Kumar
            Date of Birth: 10-02-1998
            Time of Birth: 10:30 AM
            Place of Birth: Hyderabad
            Height: 5.8
            Qualification: B.Tech
            Occupation: Software Engineer
            Current Location: Bangalore
            Father Name: Ramesh Kumar
            Mother Name: Lakshmi
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Aryan Kumar", profile.getFullName());
        assertEquals("10-02-1998", profile.getDateOfBirth());
        assertEquals("10:30 AM", profile.getTimeOfBirth());
        assertEquals("Hyderabad", profile.getPlaceOfBirth());
        assertEquals("5.8", profile.getHeight());
        assertEquals("B.Tech", profile.getQualification());
        assertEquals("Software Engineer", profile.getOccupation());
        assertEquals("Bangalore", profile.getCurrentLocation());
        assertEquals("Ramesh Kumar", profile.getFatherName());
        assertEquals("Lakshmi", profile.getMotherName());
    }

    @Test
    void shouldParseAliasLabels() {
        String rawText = """
            Bride Name: Sravya
            Profession: Doctor
            DOB: 15-05-1997
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Sravya", profile.getFullName());
        assertEquals("Doctor", profile.getOccupation());
        assertEquals("15-05-1997", profile.getDateOfBirth());
    }

    @Test
    void shouldHandleMissingFields() {
        String rawText = """
            Name: Aryan
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Aryan", profile.getFullName());
        assertNull(profile.getOccupation());
        assertNull(profile.getDateOfBirth());
    }

    @Test
    void shouldParseSingleLineWithPipeDelimiters() {
        String rawText = "Name: Manasa | DOB: 02/11/1998 | Height: 5'4 | Caste: Padmashali | Job: Oracle Fusion Developer | Package: 11 LPA";

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Manasa", profile.getFullName());
        assertEquals("02/11/1998", profile.getDateOfBirth());
        assertEquals("5'4", profile.getHeight());
        assertEquals("Padmashali", profile.getCaste());
        assertEquals("Oracle Fusion Developer", profile.getOccupation());
        assertEquals("11 LPA", profile.getSalary());
    }

    @Test
    void shouldParseSingleLineWithCommaDelimiters() {
        String rawText = "Bride: Manasa, Education: B.Tech ECE, Job: SDE @ Amazon, Salary: 18 LPA, Native: Warangal";

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Manasa", profile.getFullName());
        assertEquals("B.Tech ECE", profile.getQualification());
        assertEquals("SDE", profile.getOccupation());
        assertEquals("Amazon", profile.getCompany());
        assertEquals("18 LPA", profile.getSalary());
        assertEquals("Warangal", profile.getNativePlace());
    }

    @Test
    void shouldParseInlineWithoutExplicitDelimiters() {
        String rawText = "Full Name: Sravan DOB: 10-04-1995 Gothram: Shivaiah Caste: Kamma Salary: 15 LPA";

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Sravan", profile.getFullName());
        assertEquals("10-04-1995", profile.getDateOfBirth());
        assertEquals("Shivaiah", profile.getGothram());
        assertEquals("Kamma", profile.getCaste());
        assertEquals("15 LPA", profile.getSalary());
    }

    @Test
    void shouldParseTeluguTransliteratedFields() {
        String rawText = """
            Abbai Peru: Sai Teja
            Putinadinam: 05-08-1995
            Chadavu: B.Tech ECE
            Udhyogam: Senior Software Engineer
            Kulam: Arya Vysya
            Gothram: Madanagopala
            Thandri Peru: Satyanarayana
            Thalli Peru: Sujatha
            Swasthalam: Karimnagar
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Sai Teja", profile.getFullName());
        assertEquals("05-08-1995", profile.getDateOfBirth());
        assertEquals("B.Tech ECE", profile.getQualification());
        assertEquals("Senior Software Engineer", profile.getOccupation());
        assertEquals("Arya Vysya", profile.getCaste());
        assertEquals("Madanagopala", profile.getGothram());
        assertEquals("Satyanarayana", profile.getFatherName());
        assertEquals("Sujatha", profile.getMotherName());
        assertEquals("Karimnagar", profile.getNativePlace());
    }

    @Test
    void shouldDisambiguateFatherAndMotherFromCandidateDetails() {
        String rawText = """
            Name: Rajesh
            Occupation: IT Analyst
            Father Name: Sri Venkateshwar Rao
            Father Occupation: Retired Govt Employee
            Mother Name: Smt Vanaja
            Mother Occupation: Homemaker
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Rajesh", profile.getFullName());
        assertEquals("IT Analyst", profile.getOccupation());
        assertEquals("Sri Venkateshwar Rao", profile.getFatherName());
        assertEquals("Retired Govt Employee", profile.getFatherOccupation());
        assertEquals("Smt Vanaja", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
    }

    @Test
    void shouldHandleDirtyFormattingWithBulletsAndDelimiters() {
        String rawText = """
            * 1. Name - Manasa Devarashetty
            * 2. Date of Birth : 02-11-1998
            • 3. Height = 5 feet 4 inches
            • 4. Qualification : B.Tech (ECE)
            - 5. Company Name : Capgemini Technologies
            - 6. D/O: Sri Devarashetty Mahender
            """;

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Manasa Devarashetty", profile.getFullName());
        assertEquals("02-11-1998", profile.getDateOfBirth());
        assertEquals("5 feet 4 inches", profile.getHeight());
        assertEquals("B.Tech (ECE)", profile.getQualification());
        assertEquals("Capgemini Technologies", profile.getCompany());
        assertEquals("Sri Devarashetty Mahender", profile.getFatherName());
    }

    @Test
    void shouldHandleMixedCaseAndMessySpacing() {
        String rawText = "nAmE  :  Manasa   d.O.B: 02/11/1998   hEiGhT: 5'4   jOb: SDE";

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Manasa", profile.getFullName());
        assertEquals("02/11/1998", profile.getDateOfBirth());
        assertEquals("5'4", profile.getHeight());
        assertEquals("SDE", profile.getOccupation());
    }

    @Test
    void shouldHandleTeluguAndEnglishMixedSingleLine() {
        String rawText = "Abbai Peru: Sai Kiran | DOB: 12-08-1994 | Chadavu: M.Tech | Company: Microsoft | Salary: 35 LPA | Gothram: Kashyapa";

        ProfileBiodata profile = parser.parse(rawText);

        assertEquals("Sai Kiran", profile.getFullName());
        assertEquals("12-08-1994", profile.getDateOfBirth());
        assertEquals("M.Tech", profile.getQualification());
        assertEquals("Microsoft", profile.getCompany());
        assertEquals("35 LPA", profile.getSalary());
        assertEquals("Kashyapa", profile.getGothram());
    }

    @Test
    void shouldHandleNullOrEmptyInputSafely() {
        ProfileBiodata emptyProfile = parser.parse("");
        assertNotNull(emptyProfile);
        assertNull(emptyProfile.getFullName());

        ProfileBiodata nullProfile = parser.parse(null);
        assertNotNull(nullProfile);
        assertNull(nullProfile.getFullName());
    }

    @Test
    void shouldParseJsonRawTextProfile1() {
        String jsonProfile = """
            {
              "profile_type": "groom",
              "source": "biodata_1",
              "code": "Direct",
              "caste": "Kapu",
              "surname": "Pasupul",
              "name": "Zenith",
              "date_of_birth": "30-03-2000",
              "time_of_birth": "8:27 AM",
              "place_of_birth": "Hyderabad",
              "height": "5.8",
              "complexion": "Fair",
              "rasi": "Makara",
              "nakshatram": "Shravana",
              "gothram": "Janakula",
              "education": "B.Tech (Mechanical)",
              "occupation": "Business Development Manager",
              "company": "W3Globals, DCL",
              "work_location": "Abacus IT Park, Hyderabad",
              "salary": "8 lakhs PA",
              "family": {
                "father": {
                  "name": "Venugopal",
                  "occupation": "Business"
                },
                "mother": {
                  "name": "Sunitha",
                  "occupation": "Home Maker"
                }
              },
              "native_place": "Guntur"
            }
            """;

        ProfileBiodata profile = parser.parse(jsonProfile);

        assertEquals("Pasupul Zenith", profile.getFullName());
        assertEquals("30-03-2000", profile.getDateOfBirth());
        assertEquals("8:27 AM", profile.getTimeOfBirth());
        assertEquals("Hyderabad", profile.getPlaceOfBirth());
        assertEquals("5.8", profile.getHeight());
        assertEquals("Kapu", profile.getCaste());
        assertEquals("Makara", profile.getRashi());
        assertEquals("Shravana", profile.getNakshatram());
        assertEquals("Janakula", profile.getGothram());
        assertEquals("B.Tech (Mechanical)", profile.getQualification());
        assertEquals("Business Development Manager", profile.getOccupation());
        assertEquals("W3Globals, DCL", profile.getCompany());
        assertEquals("8 lakhs PA", profile.getSalary());
        assertEquals("Venugopal", profile.getFatherName());
        assertEquals("Business", profile.getFatherOccupation());
        assertEquals("Sunitha", profile.getMotherName());
        assertEquals("Home Maker", profile.getMotherOccupation());
        assertEquals("Guntur", profile.getNativePlace());
    }

    @Test
    void shouldParseJsonRawTextProfile2() {
        String jsonProfile = """
            {
              "profile_type": "groom",
              "source": "biodata_2",
              "surname": "Gottipalli",
              "name": "Venkata Sainadh",
              "full_name": "Venkata Sainadh Gottipalli",
              "date_of_birth": "15-10-1991",
              "time_of_birth": "3:26 AM",
              "place_of_birth": "Payakaraopeta, Anakapalli Dist.",
              "height": "5'9",
              "complexion": "Fair",
              "rasi": "Dhanur Raasi",
              "nakshatram": "Purvashada",
              "caste": "Kapu Telukula",
              "gothram": "Nagula",
              "education": "B.Tech (CSE) / JNTU-K",
              "occupation": "IT Software",
              "designation": "Associate Architect",
              "company": "Virtusa Corporation (FTE), JPMorgan Chase & Co. (Citi)",
              "work_location": "Plano, Dallas, TX, United States",
              "salary": "$130,000 per annum",
              "family": {
                "father": {
                  "name": "Venkata S Vara Prasad Rao Gottipalli",
                  "occupation": "LIC Development Officer (Retd.)"
                },
                "mother": {
                  "name": "Santhakumari Gottipalli",
                  "occupation": "Home Maker"
                }
              }
            }
            """;

        ProfileBiodata profile = parser.parse(jsonProfile);

        assertEquals("Venkata Sainadh Gottipalli", profile.getFullName());
        assertEquals("15-10-1991", profile.getDateOfBirth());
        assertEquals("3:26 AM", profile.getTimeOfBirth());
        assertEquals("Payakaraopeta, Anakapalli Dist", profile.getPlaceOfBirth());
        assertEquals("5'9", profile.getHeight());
        assertEquals("Kapu Telukula", profile.getCaste());
        assertEquals("Nagula", profile.getGothram());
        assertEquals("Dhanur", profile.getRashi());
        assertEquals("Purvashada", profile.getNakshatram());
        assertEquals("B.Tech (CSE) / JNTU-K", profile.getQualification());
        assertEquals("IT Software", profile.getOccupation());
        assertEquals("$130,000 per annum", profile.getSalary());
        assertEquals("Venkata S Vara Prasad Rao Gottipalli", profile.getFatherName());
        assertEquals("LIC Development Officer (Retd.)", profile.getFatherOccupation());
        assertEquals("Santhakumari Gottipalli", profile.getMotherName());
        assertEquals("Home Maker", profile.getMotherOccupation());
    }

    @Test
    void shouldParseJsonRawTextProfile3() {
        String jsonProfile = """
            {
              "profile_type": "groom",
              "source": "biodata_3_and_4",
              "surname": "Thota",
              "name": "Rohan Thota",
              "date_of_birth": "24-06-1997",
              "time_of_birth": "7:00 PM",
              "place_of_birth": "Nizamabad",
              "height": "6 feet",
              "rasi": "Makara",
              "nakshatram": "Dhanishta",
              "gothram": "Kashyapa",
              "caste": "Munnuru Kapu",
              "occupation": "Assistant Manager",
              "company": "CIBC Mellon",
              "annual_package": "CAD 100K",
              "current_location": "Toronto, Canada",
              "family": {
                "father": {
                  "name": "Ravinder Thota",
                  "occupation": "COO - Chief Operating Officer"
                },
                "mother": {
                  "name": "Vanitha",
                  "occupation": "Homemaker"
                }
              }
            }
            """;

        ProfileBiodata profile = parser.parse(jsonProfile);

        assertEquals("Rohan Thota", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("7:00 PM", profile.getTimeOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6 feet", profile.getHeight());
        assertEquals("Makara", profile.getRashi());
        assertEquals("Dhanishta", profile.getNakshatram());
        assertEquals("Kashyapa", profile.getGothram());
        assertEquals("Munnuru Kapu", profile.getCaste());
        assertEquals("Assistant Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("CAD 100K", profile.getSalary());
        assertEquals("Toronto, Canada", profile.getCurrentLocation());
        assertEquals("Ravinder Thota", profile.getFatherName());
        assertEquals("COO - Chief Operating Officer", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
    }

    @Test
    void shouldParseNativeTeluguScriptProfileAndHandleCompoundFatherOccupation() {
        String teluguBiodata = """
                పేరు: మున్నూరు రాజేష్
                పుట్టిన తేది: 24-06-1997
                పుట్టిన సమయం: 7:00 PM
                పుట్టిన స్థలం: నిజామాబాద్
                ఎత్తు: 6 అడుగులు
                కులం: మున్నూరు కాపు
                గోత్రం: కశ్యప
                రాశి: మకర
                నక్షత్రం: ధనిష్ట
                చదువు: బి.టెక్ (మెకానికల్)
                ఉద్యోగం: సాఫ్ట్‌వేర్ ఇంజనీర్
                కంపెనీ: గూగుల్
                జీతం: 24 LPA
                తండ్రి పేరు: రవీందర్
                తండ్రి ఉద్యోగం: వ్యాపారం
                తల్లి పేరు: వనిత
                తల్లి ఉద్యోగం: గృహిణి
                స్వస్థలం: నిజామాబాద్
                """;

        ExtractionResultDTO result = parser.parseBiodata(teluguBiodata);
        ProfileBiodata profile = result.getProfile();

        assertEquals("మున్నూరు రాజేష్", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("7:00 PM", profile.getTimeOfBirth());
        assertEquals("నిజామాబాద్", profile.getPlaceOfBirth());
        assertEquals("6 అడుగులు", profile.getHeight());
        assertEquals("మున్నూరు కాపు", profile.getCaste());
        assertEquals("కశ్యప", profile.getGothram());
        assertEquals("మకర", profile.getRashi());
        assertEquals("ధనిష్ట", profile.getNakshatram());
        assertEquals("బి.టెక్ (మెకానికల్)", profile.getQualification());
        assertEquals("సాఫ్ట్‌వేర్ ఇంజనీర్", profile.getOccupation());
        assertEquals("గూగుల్", profile.getCompany());
        assertEquals("24 LPA", profile.getSalary());

        // CRITICAL CHECK: Compound "తండ్రి ఉద్యోగం" must attribute to fatherOccupation, NOT fatherName!
        assertEquals("రవీందర్", profile.getFatherName());
        assertEquals("వ్యాపారం", profile.getFatherOccupation());
        assertEquals("వనిత", profile.getMotherName());
        assertEquals("గృహిణి", profile.getMotherOccupation());
        assertEquals("నిజామాబాద్", profile.getNativePlace());

        // Confidence checks
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fullName"));
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fatherOccupation"));
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("caste"));
    }

    @Test
    void shouldCaptureUnparsedLinesAndIgnoreDecorations() {
        String inputWithNotes = """
                ==============================
                *** MATRIMONIAL BIODATA ***
                ==============================
                Name: Manasa
                DOB: 02-11-1998
                Education: B.Tech
                Job: SDE @ Amazon
                Package: 18 LPA
                ------------------------------
                Looking for alliances in Hyderabad or USA only.
                Own 3BHK flat in Kukatpally.
                Mother late, father remarried.
                ==============================
                """;

        ExtractionResultDTO result = parser.parseBiodata(inputWithNotes);

        assertEquals("Manasa", result.getProfile().getFullName());
        assertEquals("02-11-1998", result.getProfile().getDateOfBirth());
        assertEquals("18 LPA", result.getProfile().getSalary());

        // Verify decorative lines are NOT in unparsedLines
        assertFalse(result.getUnparsedLines().stream().anyMatch(l -> l.contains("===") || l.contains("---")));
        assertFalse(result.getUnparsedLines().stream().anyMatch(l -> l.equalsIgnoreCase("*** MATRIMONIAL BIODATA ***")));

        // Verify unmapped client notes are retained in unparsedLines
        assertTrue(result.getUnparsedLines().contains("Looking for alliances in Hyderabad or USA only."));
        assertTrue(result.getUnparsedLines().contains("Own 3BHK flat in Kukatpally."));
        assertTrue(result.getUnparsedLines().contains("Mother late, father remarried."));
    }

    @Test
    void shouldMarkBlankExtractedValuesAsMissing() {
        String inputWithBlanks = """
                Name: Rajesh
                Date of Birth:
                Salary:
                """;

        ExtractionResultDTO result = parser.parseBiodata(inputWithBlanks);

        assertEquals("Rajesh", result.getProfile().getFullName());
        assertEquals(FieldConfidence.HIGH, result.getConfidenceScores().get("fullName"));
        assertEquals(FieldConfidence.MISSING, result.getConfidenceScores().get("dateOfBirth"));
        assertEquals(FieldConfidence.MISSING, result.getConfidenceScores().get("salary"));
    }

    @Test
    void shouldParseTeluguJsonWithGrandparentsAndReferences() {
        String teluguJson = """
                {
                  "ప్రొఫైల్_రకం": "వరుడు",
                  "మూలం": "బయోడేటా_3_మరియు_4",
                  "ఇంటి_పేరు": "Thota",
                  "పేరు": "Rohan Thota",
                  "పుట్టిన_తేదీ": "24-06-1997",
                  "పుట్టిన_సమయం": "సాయంత్రం 7:00 గంటలకు",
                  "పుట్టిన_స్థలం": "Nizamabad",
                  "ఎత్తు": "6 అడుగులు",
                  "ఛాయ": "గోధుమ రంగు",
                  "రాశి": "మకర రాశి",
                  "నక్షత్రం": "ధనిష్ఠ",
                  "గోత్రం": "కాశ్యప",
                  "కులం": "మున్నూరు కాపు",
                  "వైవాహిక_స్థితి": "ఇంతవరకు వివాహం కాలేదు",
                  "విద్య": [
                    "BBA",
                    "PGD in Financial Planning"
                  ],
                  "వృత్తి": "Assistant Manager",
                  "సంస్థ": "CIBC Mellon",
                  "వార్షిక_వేతనం": "CAD 100K",
                  "ప్రస్తుత_నివాస_స్థలం": "Toronto, Canada",
                  "నివాస_స్థితి": "Permanent Resident (PR)",
                  "కుటుంబం": {
                    "తండ్రి": {
                      "పేరు": "Ravinder Thota",
                      "వృత్తి": "COO - Chief Operating Officer",
                      "సంస్థ": "Embedded IT Solutions",
                      "ప్రదేశం": "Hyderabad",
                      "స్వస్థలం": "Yamcha, Nizamabad"
                    },
                    "తల్లి": {
                      "పేరు": "Vanitha",
                      "వృత్తి": "గృహిణి"
                    },
                    "తల్లిదండ్రుల_ఇంటి_పేరు": "Akula",
                    "తల్లిదండ్రుల_స్వస్థలం": "Manikbhandar, Nizamabad",
                    "తల్లిదండ్రుల_నివాస_స్థలం": "Suchitra, Hyderabad",
                    "ఆస్తులు_మరియు_ఆర్థిక_స్థితి": "సుఖసంతోషాలతో స్థిరపడిన కుటుంబం",
                    "తాత_ముత్తాతల_వివరాలు": {
                      "తండ్రి_తరపు": {
                        "తాత": {
                          "పేరు": "Thota Dasharatham",
                          "స్థితి": "స్వర్గీయులు",
                          "వృత్తి": "విరమణ పొందిన ప్రధానోపాధ్యాయులు"
                        },
                        "అమ్మమ్మ": {
                          "పేరు": "Kamala Bai",
                          "ప్రదేశం": "Yamcha, Nizamabad"
                        }
                      },
                      "తల్లి_తరపు": {
                        "తాత": {
                          "పేరు": "Akula Hanmandu"
                        },
                        "అమ్మమ్మ": {
                          "పేరు": "Akula Suguna"
                        }
                      }
                    },
                    "తోబుట్టువులు": [
                      {
                        "సంబంధం": "అన్నయ్య",
                        "పేరు": "Rohil Thota",
                        "వృత్తి": "Software Engineer",
                        "ప్రదేశం": "USA",
                        "వైవాహిక_స్థితి": "వివాహితుడు",
                        "భార్య": {
                          "పేరు": "T. Snehitha",
                          "వృత్తి": "USAలో MS పూర్తి చేసి, Bank of Americaలో ఉద్యోగం"
                        }
                      }
                    ]
                  },
                  "జీవిత_భాగస్వామి_ఎంపికలు": {
                    "వయస్సు": "2 నుండి 3 సంవత్సరాలు చిన్నవారు",
                    "ఎత్తు": "5'3\\" మరియు అంతకంటే ఎక్కువ",
                    "వృత్తి": "ఏదైనా",
                    "ప్రదేశం": "ఏదైనా",
                    "ఆస్తి_మరియు_ఆదాయ_అంచనాలు": "సుఖసంతోషాలతో స్థిరపడిన కుటుంబం"
                  },
                  "సూచనలు": [
                    {
                      "పేరు": "Smt. Akula Lalitha",
                      "సమాజం": "మున్నూరు కాపు సంఘం",
                      "హోదా": "మాజీ MLC",
                      "ప్రదేశం": "Hyderabad"
                    },
                    {
                      "పేరు": "Sri. Nakka Ramprasad Patel",
                      "సమాజం": "మున్నూరు కాపు సంఘం",
                      "హోదా": "వ్యాపారవేత్త",
                      "ప్రదేశం": "Hyderabad"
                    }
                  ],
                  "గమనిక": "ఈ బయోడేటాలోని సమాచారం తల్లిదండ్రులు లేదా సంరక్షకులు అందించినది."
                }
                """;

        ExtractionResultDTO result = parser.parseBiodata(teluguJson);
        ProfileBiodata profile = result.getProfile();

        System.out.println("FULL NAME: " + profile.getFullName());
        System.out.println("DOB: " + profile.getDateOfBirth());
        System.out.println("TOB: " + profile.getTimeOfBirth());
        System.out.println("POB: " + profile.getPlaceOfBirth());
        System.out.println("HEIGHT: " + profile.getHeight());
        System.out.println("CASTE: " + profile.getCaste());
        System.out.println("GOTHRAM: " + profile.getGothram());
        System.out.println("RASHI: " + profile.getRashi());
        System.out.println("QUALIFICATION: " + profile.getQualification());
        System.out.println("NAKSHATRAM: " + profile.getNakshatram());
        System.out.println("OCCUPATION: " + profile.getOccupation());
        System.out.println("COMPANY: " + profile.getCompany());
        System.out.println("SALARY: " + profile.getSalary());
        System.out.println("CURRENT LOCATION: " + profile.getCurrentLocation());
        System.out.println("FATHER NAME: " + profile.getFatherName());
        System.out.println("FATHER OCCUPATION: " + profile.getFatherOccupation());
        System.out.println("MOTHER NAME: " + profile.getMotherName());
        System.out.println("MOTHER OCCUPATION: " + profile.getMotherOccupation());
        System.out.println("SIBLINGS: " + profile.getSiblingsDetails());
        System.out.println("UNPARSED LINES: " + result.getUnparsedLines());

        assertEquals("Rohan Thota", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("సాయంత్రం 7:00 గంటలకు", profile.getTimeOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6 అడుగులు", profile.getHeight());
        assertEquals("మున్నూరు కాపు", profile.getCaste());
        assertEquals("కాశ్యప", profile.getGothram());
        assertEquals("మకర", profile.getRashi());
        assertEquals("ధనిష్ఠ", profile.getNakshatram());
        assertEquals("BBA, PGD in Financial Planning", profile.getQualification());
        assertEquals("Assistant Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("CAD 100K", profile.getSalary());
        assertEquals("Toronto, Canada", profile.getCurrentLocation());
        assertEquals("Ravinder Thota", profile.getFatherName());
        assertEquals("COO - Chief Operating Officer", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("గృహిణి", profile.getMotherOccupation());
    }

    @Test
    void shouldParseUserPostmanInput() {
        String input = """
                PERSONAL DETAILS

                Name - Rohan Thota
                Date Of Birth : 24-06-1997
                Born at Nizamabad
                Height : 6 feet

                Educational & Professional Details

                Education: BBA
                PGD in Financial Planning
                Working as Assistant Manager at CIBC Mellon

                Family Details

                Father's Name : Ravinder Thota
                Father is COO at Embedded IT Solutions
                Mother : Vanitha
                Homemaker
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Rohan Thota", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertNull(profile.getTimeOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6 feet", profile.getHeight());
        assertEquals("BBA, PGD in Financial Planning", profile.getQualification());
        assertEquals("Assistant Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("Ravinder Thota", profile.getFatherName());
        assertEquals("COO at Embedded IT Solutions", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
    }

    @Test
    void shouldParseWhatsAppStyleBiodata() {
        String input = """
                ROHAN THOTA
                DOB 24-06-1997
                Nizamabad born
                6ft

                B.Tech JNTU
                Asst Manager @ CIBC Mellon
                Toronto

                Father Ravinder - COO Embedded IT
                Mother Vanitha - Home Maker
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("ROHAN THOTA", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6ft", profile.getHeight());
        assertEquals("B.Tech JNTU", profile.getQualification());
        assertEquals("Asst Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("Ravinder", profile.getFatherName());
        assertEquals("COO Embedded IT", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Home Maker", profile.getMotherOccupation());
        assertTrue(result.getUnparsedLines().contains("Toronto"));
    }

    @Test
    void shouldParsePipeDelimitedBiodata() {
        String input = """
                ROHAN THOTA | DOB 24-06-1997 | Born at Nizamabad | Height 6ft
                Education B.Tech JNTU | Working as Assistant Manager at CIBC Mellon
                Location Toronto
                Father: Ravinder | COO Embedded IT
                Mother: Vanitha | Homemaker
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("ROHAN THOTA", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6ft", profile.getHeight());
        assertEquals("B.Tech JNTU", profile.getQualification());
        assertEquals("Assistant Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("Toronto", profile.getCurrentLocation());
        assertEquals("Ravinder", profile.getFatherName());
        assertEquals("COO Embedded IT", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertTrue(result.getUnparsedLines().isEmpty());
    }

    @Test
    void shouldParseProfile3And4WithMotherDetails() {
        String input = """
                "profile_type": "groom",
                      "source": "biodata_3_and_4",
                      "surname": "Thota",
                      "name": "Rohan Thota",
                      "date_of_birth": "24-06-1997",
                      "time_of_birth": "7:00 PM",
                      "place_of_birth": "Nizamabad",
                      "height": "6 feet",
                      "complexion": "Wheatish",
                      "rasi": "Makara",
                      "nakshatram": "Dhanishta",
                      "gothram": "Kashyapa",
                      "caste": "Munnuru Kapu",
                      "marital_status": "Never Married",
                      "education": [
                        "BBA",
                        "PGD in Financial Planning"
                      ],
                      "occupation": "Assistant Manager",
                      "company": "CIBC Mellon",
                      "annual_package": "CAD 100K",
                      "current_location": "Toronto, Canada",
                      "residency_status": "Permanent Resident (PR)",
                      "family": {
                        "father": {
                          "name": "Ravinder Thota",
                          "occupation": "COO - Chief Operating Officer",
                          "company": "Embedded IT Solutions",
                          "location": "Hyderabad",
                          "native_place": "Yamcha, Nizamabad"
                        },
                        "mother": {
                          "name": "Vanitha",
                          "occupation": "Homemaker"
                        },
                        "parents_surname": "Akula",
                        "parents_native_place": "Manikbhandar, Nizamabad",
                        "parents_residence": "Suchitra, Hyderabad",
                        "assets_property": "Well-Settled Family",
                        "grandparents": {
                          "paternal": {
                            "grandfather": {
                              "name": "Thota Dasharatham",
                              "status": "Late",
                              "occupation": "Retired Headmaster"
                            },
                            "grandmother": {
                              "name": "Kamala Bai",
                              "location": "Yamcha, Nizamabad"
                            }
                          },
                          "maternal": {
                            "grandfather": {
                              "name": "Akula Hanmandu"
                            },
                            "grandmother": {
                              "name": "Akula Suguna"
                            }
                          }
                        },
                        "siblings": [
                          {
                            "relation": "Elder Brother",
                            "name": "Rohil Thota",
                            "profession": "Software Engineer",
                            "location": "USA",
                            "marital_status": "Married",
                            "spouse": {
                              "name": "T. Snehitha",
                              "profession": "MS in USA, Working for Bank of America"
                            }
                          }
                        ]
                      },
                      "partner_preferences": {
                        "age": "2 to 3 years younger",
                        "height": "5'3\\" and above",
                        "occupation": "Any",
                        "location": "Any",
                        "property_income_expectations": "Well Settled Family"
                      },
                      "references": [
                        {
                          "name": "Smt. Akula Lalitha",
                          "community": "Munnuru Kapu Community",
                          "designation": "Ex-MLC",
                          "location": "Hyderabad"
                        },
                        {
                          "name": "Sri. Nakka Ramprasad Patel",
                          "community": "Munnuru Kapu Community",
                          "designation": "Businessman",
                          "location": "Hyderabad"
                        }
                      ],
                      "disclaimer": "The information provided is furnished by the parent or guardian. PEN NETWORK assumes no responsibility for any misrepresentation of the facts."
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Rohan Thota", profile.getFullName());
        assertEquals("Ravinder Thota", profile.getFatherName());
        assertEquals("COO - Chief Operating Officer", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertTrue(profile.getSiblingsDetails().contains("Rohil Thota"));
        assertFalse(profile.getSiblingsDetails().contains("Nakka"));
    }

    @Test
    void shouldParseCategory1NormalLabelledEnglish() {
        String input = """
                MATRIMONIAL BIODATA

                Name: Sai Teja Reddy
                Date of Birth: 14-08-1996
                Time of Birth: 06:45 AM
                Place of Birth: Karimnagar
                Height: 5ft 11in
                Caste: Reddy
                Gothram: Sanathana
                Rashi: Simha
                Nakshatram: Magha
                Education: B.Tech, MS in Computer Science
                Occupation: Senior Software Engineer
                Company: Amazon
                Salary: 28 LPA
                Current Location: Hyderabad

                Family Details
                Father Name: Narayana Reddy
                Father Occupation: Executive Engineer (Retd)
                Mother Name: Sunitha
                Mother Occupation: Homemaker
                Native Place: Huzurabad, Karimnagar
                Siblings: 1 Younger Brother (Software Engineer at Infosys)
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Sai Teja Reddy", profile.getFullName());
        assertEquals("14-08-1996", profile.getDateOfBirth());
        assertEquals("06:45 AM", profile.getTimeOfBirth());
        assertEquals("Karimnagar", profile.getPlaceOfBirth());
        assertEquals("5ft 11in", profile.getHeight());
        assertEquals("Reddy", profile.getCaste());
        assertEquals("Sanathana", profile.getGothram());
        assertEquals("Simha", profile.getRashi());
        assertEquals("Magha", profile.getNakshatram());
        assertEquals("B.Tech, MS in Computer Science", profile.getQualification());
        assertEquals("Senior Software Engineer", profile.getOccupation());
        assertEquals("Amazon", profile.getCompany());
        assertEquals("28 LPA", profile.getSalary());
        assertEquals("Hyderabad", profile.getCurrentLocation());
        assertEquals("Narayana Reddy", profile.getFatherName());
        assertEquals("Executive Engineer (Retd)", profile.getFatherOccupation());
        assertEquals("Sunitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertEquals("Huzurabad, Karimnagar", profile.getNativePlace());
        assertTrue(profile.getSiblingsDetails().contains("Younger Brother"));
    }

    @Test
    void shouldParseCategory2MessyLabelledEnglish() {
        String input = """
                *** BIO DATA ***
                * Candidate Name - Priyanka Sharma
                * D.O.B ~ 22/11/1997
                * Birth Time - 02:15 PM
                * Birth Place - Warangal
                * Height : 5'4"
                * Community - Brahmin
                * Gothram - Vashishta
                * Star - Rohini
                * Raasi - Vrishabha
                * Qualification - B.Com, MBA Finance
                * Job - Financial Analyst @ Deloitte
                * Annual Income - 14 LPA
                * Living in - Bangalore
                * Father: Satyanarayana Sharma - Advocate
                * Mother: Gayatri - Teacher
                * Native : Hanamkonda
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Priyanka Sharma", profile.getFullName());
        assertEquals("22/11/1997", profile.getDateOfBirth());
        assertEquals("02:15 PM", profile.getTimeOfBirth());
        assertEquals("Warangal", profile.getPlaceOfBirth());
        assertEquals("5'4", profile.getHeight());
        assertEquals("Brahmin", profile.getCaste());
        assertEquals("Vashishta", profile.getGothram());
        assertEquals("Rohini", profile.getNakshatram());
        assertEquals("Vrishabha", profile.getRashi());
        assertEquals("B.Com, MBA Finance", profile.getQualification());
        assertEquals("Financial Analyst", profile.getOccupation());
        assertEquals("Deloitte", profile.getCompany());
        assertEquals("14 LPA", profile.getSalary());
        assertEquals("Bangalore", profile.getCurrentLocation());
        assertEquals("Satyanarayana Sharma", profile.getFatherName());
        assertEquals("Advocate", profile.getFatherOccupation());
        assertEquals("Gayatri", profile.getMotherName());
        assertEquals("Teacher", profile.getMotherOccupation());
        assertEquals("Hanamkonda", profile.getNativePlace());
    }

    @Test
    void shouldParseCategory3SingleLineLabelled() {
        String input = "Name: Sahith Alwala | DOB: 15-08-1996 | Height: 5ft 10in | Education: B.Tech CSE | Profession: Lead Consultant at Capgemini | Salary: 20 LPA | City: Pune | Caste: Munnuru Kapu | Gothram: Kashyapa | Father: Srikanth Alwala (Business) | Mother: Madhavi (Homemaker) | Native: Nizamabad";

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Sahith Alwala", profile.getFullName());
        assertEquals("15-08-1996", profile.getDateOfBirth());
        assertEquals("5ft 10in", profile.getHeight());
        assertEquals("B.Tech CSE", profile.getQualification());
        assertEquals("Lead Consultant", profile.getOccupation());
        assertEquals("Capgemini", profile.getCompany());
        assertEquals("20 LPA", profile.getSalary());
        assertEquals("Pune", profile.getCurrentLocation());
        assertEquals("Munnuru Kapu", profile.getCaste());
        assertEquals("Kashyapa", profile.getGothram());
        assertEquals("Srikanth Alwala", profile.getFatherName());
        assertEquals("Business", profile.getFatherOccupation());
        assertEquals("Madhavi", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertEquals("Nizamabad", profile.getNativePlace());
    }

    @Test
    void shouldParseCategory4PureTeluguLabelled() {
        String input = """
                వివాహ పరిచయ వేదిక - బయోడేటా

                పేరు: కొండ సురేష్
                పుట్టిన తేదీ: 18-05-1995
                పుట్టిన సమయం: ఉదయం 8:30 గంటలకు
                పుట్టిన స్థలం: ఖమ్మం
                ఎత్తు: 5 అడుగుల 9 అంగుళాలు
                కులం: పద్మశాలి
                గోత్రం: మార్కండేయ
                రాశి: ధనుస్సు
                నక్షత్రం: మూల
                చదువు: ఎం.సి.ఎ (MCA)
                ఉద్యోగం: సాఫ్ట్‌వేర్ డెవలపర్
                కంపెనీ: టి.సి.ఎస్ (TCS)
                జీతం: 16 లక్షలు వార్షికం
                ప్రస్తుత నివాసం: హైదరాబాద్

                కుటుంబ వివరాలు
                తండ్రి పేరు: కొండ మల్లయ్య
                తండ్రి ఉద్యోగం: రిటైర్డ్ ప్రభుత్వ ఉద్యోగి
                తల్లి పేరు: కొండ లక్ష్మి
                తల్లి ఉద్యోగం: గృహిణి
                స్వస్థలం: ఖమ్మం
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("కొండ సురేష్", profile.getFullName());
        assertEquals("18-05-1995", profile.getDateOfBirth());
        assertEquals("ఉదయం 8:30 గంటలకు", profile.getTimeOfBirth());
        assertEquals("ఖమ్మం", profile.getPlaceOfBirth());
        assertEquals("5 అడుగుల 9 అంగుళాలు", profile.getHeight());
        assertEquals("పద్మశాలి", profile.getCaste());
        assertEquals("మార్కండేయ", profile.getGothram());
        assertEquals("ధనుస్సు", profile.getRashi());
        assertEquals("మూల", profile.getNakshatram());
        assertEquals("ఎం.సి.ఎ (MCA)", profile.getQualification());
        assertEquals("సాఫ్ట్‌వేర్ డెవలపర్", profile.getOccupation());
        assertEquals("టి.సి.ఎస్ (TCS)", profile.getCompany());
        assertEquals("16 లక్షలు వార్షికం", profile.getSalary());
        assertEquals("హైదరాబాద్", profile.getCurrentLocation());
        assertEquals("కొండ మల్లయ్య", profile.getFatherName());
        assertEquals("రిటైర్డ్ ప్రభుత్వ ఉద్యోగి", profile.getFatherOccupation());
        assertEquals("కొండ లక్ష్మి", profile.getMotherName());
        assertEquals("గృహిణి", profile.getMotherOccupation());
        assertEquals("ఖమ్మం", profile.getNativePlace());
    }

    @Test
    void shouldParseCategory5MixedTeluguEnglish() {
        String input = """
                బయోడేటా (BIODATA)

                పేరు: Rohan Thota
                DOB: 24-06-1997
                Time: 7:00 PM
                Place: Nizamabad
                ఎత్తు: 6 feet
                కులం: Munnuru Kapu
                గోత్రం: Kashyapa
                రాశి: Makara
                నక్షత్రం: Dhanishta
                చదువు: BBA, PGD in Financial Planning
                ఉద్యోగం: Assistant Manager at CIBC Mellon
                జీతం: CAD 100K
                Current Location: Toronto, Canada
                తండ్రి: Ravinder Thota (COO - Embedded IT)
                తల్లి: Vanitha (Homemaker)
                స్వస్థలం: Yamcha, Nizamabad
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Rohan Thota", profile.getFullName());
        assertEquals("24-06-1997", profile.getDateOfBirth());
        assertEquals("7:00 PM", profile.getTimeOfBirth());
        assertEquals("Nizamabad", profile.getPlaceOfBirth());
        assertEquals("6 feet", profile.getHeight());
        assertEquals("Munnuru Kapu", profile.getCaste());
        assertEquals("Kashyapa", profile.getGothram());
        assertEquals("Makara", profile.getRashi());
        assertEquals("Dhanishta", profile.getNakshatram());
        assertEquals("BBA, PGD in Financial Planning", profile.getQualification());
        assertEquals("Assistant Manager", profile.getOccupation());
        assertEquals("CIBC Mellon", profile.getCompany());
        assertEquals("CAD 100K", profile.getSalary());
        assertEquals("Toronto, Canada", profile.getCurrentLocation());
        assertEquals("Ravinder Thota", profile.getFatherName());
        assertEquals("COO - Embedded IT", profile.getFatherOccupation());
        assertEquals("Vanitha", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertEquals("Yamcha, Nizamabad", profile.getNativePlace());
    }

    @Test
    void shouldParseCategory7PdfExtractedText() {
        String input = """
                MATRIMONIAL BIODATA
                Page 1 of 1
                CONFIDENTIAL

                CANDIDATE DETAILS
                Name	:	Karthik Varma
                Date of Birth	:	05-11-1993
                Time of Birth	:	04:15 AM
                Place of Birth	:	Bhimavaram
                Height	:	6ft
                Caste	:	Kshatriya (Raju)
                Gothram	:	Vasista
                Education	:	B.Tech (Civil)
                Occupation	:	Assistant Executive Engineer
                Company	:	Irrigation Department
                Salary	:	12 LPA
                Location	:	Vijayawada

                FAMILY BACKGROUND
                Father's Name	:	Ramaraju Varma
                Father's Occupation	:	Agriculture & Real Estate
                Mother's Name	:	Usha Rani
                Mother's Occupation	:	Housewife
                Native Place	:	Bhimavaram, West Godavari
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Karthik Varma", profile.getFullName());
        assertEquals("05-11-1993", profile.getDateOfBirth());
        assertEquals("04:15 AM", profile.getTimeOfBirth());
        assertEquals("Bhimavaram", profile.getPlaceOfBirth());
        assertEquals("6ft", profile.getHeight());
        assertEquals("Kshatriya (Raju)", profile.getCaste());
        assertEquals("Vasista", profile.getGothram());
        assertEquals("B.Tech (Civil)", profile.getQualification());
        assertEquals("Assistant Executive Engineer", profile.getOccupation());
        assertEquals("Irrigation Department", profile.getCompany());
        assertEquals("12 LPA", profile.getSalary());
        assertEquals("Vijayawada", profile.getCurrentLocation());
        assertEquals("Ramaraju Varma", profile.getFatherName());
        assertEquals("Agriculture & Real Estate", profile.getFatherOccupation());
        assertEquals("Usha Rani", profile.getMotherName());
        assertEquals("Housewife", profile.getMotherOccupation());
        assertEquals("Bhimavaram, West Godavari", profile.getNativePlace());
    }

    @Test
    void shouldParseCategory8OcrScreenshotText() {
        String input = """
                | BIODATA |
                Name: Abhinav Reddy
                D.O.B: 28-09-1996
                Time of Birth: 09:10 PM
                Birth Place: Nalgonda
                Height: 5'8"
                Caste: Reddy
                Gothram: Chandra
                Education: B.Tech CSE
                Occupation: Systems Analyst
                Company: Cognizant
                Income: 15 LPA
                Present Location: Hyderabad
                Father Name: Mohan Reddy
                Father Occupation: Civil Contractor
                Mother Name: Radhika
                Mother Occupation: Homemaker
                Native: Suryapet
                """;

        ExtractionResultDTO result = parser.parseBiodata(input);
        ProfileBiodata profile = result.getProfile();

        assertEquals("Abhinav Reddy", profile.getFullName());
        assertEquals("28-09-1996", profile.getDateOfBirth());
        assertEquals("09:10 PM", profile.getTimeOfBirth());
        assertEquals("Nalgonda", profile.getPlaceOfBirth());
        assertEquals("5'8", profile.getHeight());
        assertEquals("Reddy", profile.getCaste());
        assertEquals("Chandra", profile.getGothram());
        assertEquals("B.Tech CSE", profile.getQualification());
        assertEquals("Systems Analyst", profile.getOccupation());
        assertEquals("Cognizant", profile.getCompany());
        assertEquals("15 LPA", profile.getSalary());
        assertEquals("Hyderabad", profile.getCurrentLocation());
        assertEquals("Mohan Reddy", profile.getFatherName());
        assertEquals("Civil Contractor", profile.getFatherOccupation());
        assertEquals("Radhika", profile.getMotherName());
        assertEquals("Homemaker", profile.getMotherOccupation());
        assertEquals("Suryapet", profile.getNativePlace());
    }
}