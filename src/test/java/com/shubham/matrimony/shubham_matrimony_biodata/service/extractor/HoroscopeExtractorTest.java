package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionContext;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionMethod;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ExtractionResult;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.FieldConfidence;
import com.shubham.matrimony.shubham_matrimony_biodata.util.BiodataField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HoroscopeExtractorTest {

    private HoroscopeExtractor extractor;
    private ParseContext ctx;

    @BeforeEach
    public void setUp() {
        extractor = new HoroscopeExtractor();
        ctx = new ParseContext();
    }

    @Test
    public void testRashiExtractionAndEvidenceEmission() {
        boolean handled = extractor.tryApply(BiodataField.RASHI, "Thula rashi", ctx);

        assertTrue(handled);
        assertEquals("Thula", ctx.profile.getRashi());

        assertEquals(1, ctx.evidenceList.size());
        ExtractionResult evidence = ctx.evidenceList.get(0);
        assertEquals(BiodataField.RASHI, evidence.getField());
        assertEquals("Thula", evidence.getValue());
        assertEquals(ExtractionContext.CANDIDATE, evidence.getContext());
        assertEquals(FieldConfidence.HIGH, evidence.getConfidence());
        assertEquals(ExtractionMethod.DETERMINISTIC, evidence.getMethod());
        assertEquals("Thula rashi", evidence.getSourceText());
    }

    @Test
    public void testNakshatramExtractionAndEvidenceEmission() {
        boolean handled = extractor.tryApply(BiodataField.NAKSHATRAM, "Vishakha nakshatram", ctx);

        assertTrue(handled);
        assertEquals("Vishakha", ctx.profile.getNakshatram());

        assertEquals(1, ctx.evidenceList.size());
        ExtractionResult evidence = ctx.evidenceList.get(0);
        assertEquals(BiodataField.NAKSHATRAM, evidence.getField());
        assertEquals("Vishakha", evidence.getValue());
        assertEquals(ExtractionContext.CANDIDATE, evidence.getContext());
    }

    @Test
    public void testGothramExtractionAndSuffixCleaning() {
        boolean handled = extractor.tryApply(BiodataField.GOTHRAM, "pasupu neti gotram", ctx);

        assertTrue(handled);
        assertEquals("pasupu neti", ctx.profile.getGothram());

        assertEquals(1, ctx.evidenceList.size());
        ExtractionResult evidence = ctx.evidenceList.get(0);
        assertEquals(BiodataField.GOTHRAM, evidence.getField());
        assertEquals("pasupu neti", evidence.getValue());
    }

    @Test
    public void testCompoundHoroscopeLine() {
        // "simha rasi , makha nakshatram"
        boolean handled = extractor.tryApply(BiodataField.RASHI, "simha rasi , makha nakshatram", ctx);

        assertTrue(handled);
        assertEquals("simha", ctx.profile.getRashi());
        assertEquals("makha", ctx.profile.getNakshatram());

        // Should emit evidence for both Rashi and Nakshatram
        assertEquals(2, ctx.evidenceList.size());
        assertTrue(ctx.evidenceList.stream()
                .anyMatch(e -> e.getField() == BiodataField.RASHI && e.getValue().equals("simha")));
        assertTrue(ctx.evidenceList.stream()
                .anyMatch(e -> e.getField() == BiodataField.NAKSHATRAM && e.getValue().equals("makha")));
    }
}
