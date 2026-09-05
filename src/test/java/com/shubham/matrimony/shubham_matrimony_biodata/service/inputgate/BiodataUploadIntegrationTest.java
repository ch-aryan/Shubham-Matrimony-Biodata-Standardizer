package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.controller.BiodataController;
import com.shubham.matrimony.shubham_matrimony_biodata.controller.BiodataExceptionHandler;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.service.BiodataParserImplementation;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BiodataUploadIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MagicByteValidator magicByteValidator = new MagicByteValidator();
        ImageSanityChecker imageSanityChecker = new ImageSanityChecker();
        PdfSanityChecker pdfSanityChecker = new PdfSanityChecker();
        BiodataParserImplementation parserService = new BiodataParserImplementation();

        InputGateService inputGateService = new InputGateService(
                magicByteValidator,
                imageSanityChecker,
                pdfSanityChecker,
                parserService,
                null // GeminiService not configured / null
        );

        BiodataController controller = new BiodataController(parserService, inputGateService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BiodataExceptionHandler())
                .build();
    }

    @Test
    void testUploadValidDigitalPdfReturnsPopulatedProfile() throws Exception {
        byte[] pdfBytes = createPdfWithBiodata();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "biodata.pdf",
                "application/pdf",
                pdfBytes
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", isOneOf("SUCCESS", "SUCCESS_WITH_WARNINGS")))
                .andExpect(jsonPath("$.profile.fullName", containsString("Satwik Kotte")))
                .andExpect(jsonPath("$.profile.occupation", containsString("Software Engineer")));
    }

    @Test
    void testUploadSolidWhiteImageReturns422Rejected() throws Exception {
        byte[] whiteImage = createSolidImage(400, 400, Color.WHITE);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blank.png",
                "image/png",
                whiteImage
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is("REJECTED_INPUT")))
                .andExpect(jsonPath("$.warnings[0].category", is(WarningCategory.BLANK_OR_UNREADABLE_IMAGE.name())));
    }

    @Test
    void testUploadDisguisedExeReturns400InvalidSignature() throws Exception {
        byte[] exeBytes = new byte[]{'M', 'Z', 0x00, 0x00, 0x01, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.pdf",
                "application/pdf",
                exeBytes
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("REJECTED_INPUT")))
                .andExpect(jsonPath("$.warnings[0].category", is(WarningCategory.INVALID_FILE_SIGNATURE.name())));
    }

    @Test
    void testUploadDocxReturns415UnsupportedMediaType() throws Exception {
        byte[] docxBytes = new byte[]{'P', 'K', 0x03, 0x04, 0x14, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "biodata.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status", is("REJECTED_INPUT")))
                .andExpect(jsonPath("$.warnings[0].category", is(WarningCategory.UNSUPPORTED_MEDIA_TYPE.name())));
    }

    @Test
    void testUploadExcessivePageCountPdfReturns400() throws Exception {
        byte[] largePdf = createMultiPagePdf(12);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "big_book.pdf",
                "application/pdf",
                largePdf
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("REJECTED_INPUT")))
                .andExpect(jsonPath("$.warnings[0].category", is(WarningCategory.EXCESSIVE_PAGE_COUNT.name())));
    }

    @Test
    void testUploadPlainTextFileReturnsPopulatedProfile() throws Exception {
        String biodataText = "Name: Rohan Sharma\nDOB: 12-04-1994\nHeight: 5ft 9in\nOccupation: Analyst";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "biodata.txt",
                "text/plain",
                biodataText.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/biodata/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", isOneOf("SUCCESS", "SUCCESS_WITH_WARNINGS")))
                .andExpect(jsonPath("$.profile.fullName", containsString("Rohan Sharma")));
    }

    private byte[] createPdfWithBiodata() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);

                String[] lines = {
                        "Name: Satwik Kotte",
                        "DOB: 15-08-1995",
                        "Height: 5ft 10in",
                        "Education: B.Tech Computer Science",
                        "Occupation: Software Engineer",
                        "Salary: 18 LPA",
                        "Father Name: S. Kotte",
                        "Mother Name: P. Kotte",
                        "Native Place: Hyderabad"
                };

                for (String line : lines) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -18);
                }
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createMultiPagePdf(int count) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < count; i++) {
                doc.addPage(new PDPage());
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createSolidImage(int w, int h, Color color) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
