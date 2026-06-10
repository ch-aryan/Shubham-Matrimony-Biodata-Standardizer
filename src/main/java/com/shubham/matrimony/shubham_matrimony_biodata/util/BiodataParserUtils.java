package com.shubham.matrimony.shubham_matrimony_biodata.util;

import java.util.Set;

public class BiodataParserUtils {

    private BiodataParserUtils() {
    }

    public static String normalizeLine(String line) {

        return line
                .toLowerCase()
                .trim();
    }

    public static boolean matchesLabel(
            String line,
            Set<String> labels) {

        String normalizedLine =
                normalizeLine(line);

        for (String label : labels) {

            if (normalizedLine.startsWith(label)) {
                return true;
            }
        }

        return false;
    }
    public static String extractValue(String line) {

        String[] parts =
                line.split("\\s*[:=-]\\s*", 2);

        if (parts.length < 2) {
            return "";
        }

        return parts[1].trim();
    }
}