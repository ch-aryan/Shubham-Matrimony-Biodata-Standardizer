package com.shubham.matrimony.shubham_matrimony_biodata.service.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw biodata text into an ordered flat list of raw (unsanitized)
 * lines.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Split on newlines ({@code \r?\n}).</li>
 * <li>Flatten pipe-separated segments on a single line
 * (e.g. {@code "Name: X | DOB: Y | Job: Z"} → 3 separate entries).</li>
 * </ul>
 *
 * <p>
 * This is the first stage of the parsing pipeline. It does NOT sanitize or
 * interpret the lines — that is left to later stages.
 */
public class InputNormalizer {

    /**
     * Splits {@code rawText} on newlines and flattens any pipe-delimited segments.
     *
     * @param rawText the raw input biodata string
     * @return ordered flat list of raw lines ready for per-line processing
     */
    public List<String> normalize(String rawText) {
        List<String> rawLines = new ArrayList<>();
        for (String line : rawText.split("\\r?\\n")) {
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
}
