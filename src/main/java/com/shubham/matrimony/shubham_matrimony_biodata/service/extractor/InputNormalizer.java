package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw biodata text into an ordered flat list of raw lines.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Unicode NFKD normalization (decomposing mathematical bold/italic/monospace
 * fonts into standard ASCII, e.g. 𝐍𝐀𝐌𝐄 → NAME, 𝟏𝟏-𝟗-𝟏𝟗𝟗𝟓 → 11-9-1995).</li>
 * <li>Split on newlines ({@code \r?\n}).</li>
 * <li>Flatten pipe-separated segments on a single line
 * (e.g. {@code "Name: X | DOB: Y | Job: Z"} → 3 separate entries).</li>
 * </ul>
 */
public class InputNormalizer {

    /**
     * Decomposes fancy Unicode characters (mathematical alphanumeric symbols),
     * splits {@code rawText} on newlines, and flattens pipe-delimited segments.
     *
     * @param rawText the raw input biodata string
     * @return ordered flat list of raw lines ready for per-line processing
     */
    public List<String> normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        // Decompose mathematical alphanumeric symbols (bold/italic) to standard ASCII
        // without decomposing Indic/Telugu vowels
        String normalizedText = normalizeStylizedFonts(rawText);

        List<String> rawLines = new ArrayList<>();
        for (String line : normalizedText.split("\\r?\\n")) {
            if (line.contains("|")) {
                // "Name: X | DOB: Y" → ["Name: X", "DOB: Y"]
                for (String part : line.split("\\|")) {
                    if (!part.isBlank()) {
                        rawLines.add(part.trim());
                    }
                }
            } else {
                rawLines.add(line);
            }
        }
        return rawLines;
    }

    /**
     * Normalizes stylized mathematical fonts (e.g. 𝐍𝐀𝐌𝐄 -> NAME, 𝟏𝟏-𝟗-𝟏𝟗𝟗𝟓 -> 11-9-1995)
     * located in Unicode Plane 1 (0x1D400 to 0x1D7FF) to standard ASCII.
     * Leaves Telugu and other Indic scripts completely untouched.
     */
    public static String normalizeStylizedFonts(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        boolean hasPlane1 = false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\uD835') {
                hasPlane1 = true;
                break;
            }
        }
        if (!hasPlane1) {
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp >= 0x1D400 && cp <= 0x1D7FF) {
                String decomp = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFKD);
                sb.append(decomp);
            } else {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }
}
