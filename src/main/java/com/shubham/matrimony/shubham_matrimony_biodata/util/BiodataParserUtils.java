package com.shubham.matrimony.shubham_matrimony_biodata.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiodataParserUtils {

    private BiodataParserUtils() {
    }

    public static class LabelRule {
        private final BiodataField field;
        private final String alias;

        public LabelRule(BiodataField field, String alias) {
            this.field = field;
            this.alias = alias.toLowerCase().trim();
        }

        public BiodataField getField() {
            return field;
        }

        public String getAlias() {
            return alias;
        }
    }

    public static class ParsedSegment {
        private final BiodataField field;
        private final String value;

        public ParsedSegment(BiodataField field, String value) {
            this.field = field;
            this.value = value;
        }

        public BiodataField getField() {
            return field;
        }

        public String getValue() {
            return value;
        }
    }

    private static class LabelMatch {
        final BiodataField field;
        final int start;
        final int end;
        final int length;

        LabelMatch(BiodataField field, int start, int end) {
            this.field = field;
            this.start = start;
            this.end = end;
            this.length = end - start;
        }
    }

    private static final List<LabelRule> COMPILED_RULES;

    static {
        List<LabelRule> rules = new ArrayList<>();
        for (BiodataField field : BiodataField.allSortedByPriority()) {
            for (String alias : field.getAliases()) {
                rules.add(new LabelRule(field, alias));
                if (alias.contains(" ")) {
                    rules.add(new LabelRule(field, alias.replace(' ', '_')));
                    rules.add(new LabelRule(field, alias.replace(' ', '-')));
                }
            }
        }
        // Sort rules so longer aliases come first, and higher priority fields come first on tie
        rules.sort((r1, r2) -> {
            int lenComp = Integer.compare(r2.getAlias().length(), r1.getAlias().length());
            if (lenComp != 0) {
                return lenComp;
            }
            return Integer.compare(r2.getField().getPriority(), r1.getField().getPriority());
        });
        COMPILED_RULES = Collections.unmodifiableList(rules);
    }

    public static String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        return line.toLowerCase().trim();
    }

    public static boolean matchesLabel(String line, Set<String> labels) {
        if (line == null || labels == null) {
            return false;
        }
        String normalizedLine = stripLeadingBullets(normalizeLine(line));

        for (String label : labels) {
            String normLabel = label.toLowerCase().trim();
            if (normalizedLine.startsWith(normLabel)) {
                // Ensure boundary after label (e.g. colon, space, delimiter, or end of string)
                if (normalizedLine.length() == normLabel.length()) {
                    return true;
                }
                char nextChar = normalizedLine.charAt(normLabel.length());
                if (isSeparatorOrWhitespace(nextChar)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String extractValue(String line) {
        if (line == null) {
            return "";
        }
        String stripped = stripLeadingBullets(line.trim());
        String[] parts = stripped.split("\\s*[:=–—~-]+\\s*", 2);

        if (parts.length < 2) {
            return "";
        }

        return cleanValue(parts[1]);
    }

    public static String cleanValue(String val) {
        if (val == null) {
            return "";
        }
        // Remove leading separators, bullets, quotes, brackets
        String cleaned = val.replaceAll("^[\\s:=–—~\\|/\\-\\.\\*\\•#\\)\\]\\}\"\'`]+", "");
        // Remove trailing separators, commas, quotes, brackets
        cleaned = cleaned.replaceAll("[\\s,\\|;~–—\\-\\.\\*\\•#\\(\\[\\{\\\"\'`]+$", "");
        // Collapse multiple whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    public static String sanitizeLine(String line) {
        if (line == null) {
            return "";
        }
        // Strip WhatsApp timestamp prefixes like "[02/11/26, 10:30 AM] Sender: " or "02/11/26, 10:30 am - Sender: "
        String cleaned = line.replaceAll("^\\[.*?\\]\\s*[^:]+:\\s*", "")
                             .replaceAll("^\\d{1,2}/\\d{1,2}/\\d{2,4},\\s*\\d{1,2}:\\d{2}\\s*(?:am|pm|AM|PM)?\\s*-\\s*[^:]+:\\s*", "")
                             .replaceAll("[\\u200B\\uFEFF]", ""); // Strip zero-width space and BOM, preserve Indic ZWNJ/ZWJ
        return stripLeadingBullets(cleaned);
    }

    public static boolean isIgnorableLine(String line) {
        if (line == null || line.isBlank()) {
            return true;
        }
        String trimmed = line.trim();
        // Decorative lines like ---, ===, ***, ____, ~~~
        if (trimmed.matches("^[\\-=\\*_~#\\.\\s]+$")) {
            return true;
        }
        // Common standalone section headers that don't hold field values
        String strippedHeader = trimmed.replaceAll("^[\\*\\-=_~#\\.\\s]+", "")
                                       .replaceAll("[\\*\\-=_~#\\.\\s:]+$", "")
                                       .toLowerCase();
        if (strippedHeader.equals("biodata") || strippedHeader.equals("bio-data") || strippedHeader.equals("bio data")
                || strippedHeader.equals("matrimonial biodata") || strippedHeader.equals("personal details")
                || strippedHeader.equals("candidate details")
                || strippedHeader.equals("confidential") || strippedHeader.matches("^page\\s*\\d+\\s*(?:of|/)\\s*\\d+$")
                || strippedHeader.equals("family details") || strippedHeader.equals("horoscope details")
                || strippedHeader.equals("educational details") || strippedHeader.equals("professional details")
                || strippedHeader.equals("educational and professional details")
                || strippedHeader.equals("sibling & marital status") || strippedHeader.equals("sibling and marital status")
                || strippedHeader.equals("family background") || strippedHeader.equals("contact details")
                || strippedHeader.equals("కుటుంబ వివరాలు") || strippedHeader.equals("వ్యక్తిగత వివరాలు")
                || strippedHeader.equals("{") || strippedHeader.equals("}") || strippedHeader.equals("},")
                || strippedHeader.equals("],") || strippedHeader.equals("]")) {
            return true;
        }
        return false;
    }

    public static boolean isConversationalNote(String lower) {
        if (lower == null || lower.isBlank()) {
            return false;
        }
        return lower.contains("remarried")
                || lower.contains("relocate")
                || lower.contains("looking for")
                || lower.contains("alliance")
                || lower.contains("alliances")
                || lower.contains("own house")
                || lower.contains("own flat")
                || lower.contains("3bhk")
                || lower.contains("2bhk")
                || lower.contains("passed away")
                || lower.contains("expired");
    }

    public static String stripLeadingBullets(String line) {
        if (line == null) {
            return "";
        }
        // Strip bullets like "1.", "1)", "*", "•", "-", etc.
        return line.replaceAll("^[\\s\\*\\•\\-\\–\\—#]+", "")
                   .replaceAll("^\\d+[\\.\\)]\\s*", "")
                   .trim();
    }

    private static boolean isSeparatorOrWhitespace(char c) {
        return Character.isWhitespace(c) || c == ':' || c == '=' || c == '-' || c == '–' || c == '—' || c == '~' || c == '|' || c == ';' || c == ',';
    }

    private static boolean isBoundaryBefore(String text, int index) {
        if (index <= 0) {
            return true;
        }
        char prev = text.charAt(index - 1);
        return Character.isWhitespace(prev) || prev == '|' || prev == ',' || prev == ';' || prev == '\n' || prev == '\r' || prev == '\t' || prev == '•' || prev == '*' || prev == '(' || prev == '[' || prev == '{' || prev == '-' || prev == '#' || prev == '.' || prev == '"' || prev == '\'' || prev == '`';
    }

    private static boolean isBoundaryAfter(String text, int index) {
        if (index >= text.length()) {
            return true;
        }
        char next = text.charAt(index);
        return isSeparatorOrWhitespace(next) || next == ')' || next == ']' || next == '}' || next == '.' || next == ',' || next == '"' || next == '\'' || next == '`';
    }

    public static List<ParsedSegment> parseTextSegments(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<LabelMatch> matches = new ArrayList<>();
        String lower = text.toLowerCase();

        // Scan for all label occurrences matching boundary conditions
        for (LabelRule rule : COMPILED_RULES) {
            String alias = rule.getAlias();
            int idx = 0;
            while ((idx = lower.indexOf(alias, idx)) != -1) {
                int endIdx = idx + alias.length();
                if (isBoundaryBefore(lower, idx) && isBoundaryAfter(lower, endIdx)) {
                    matches.add(new LabelMatch(rule.getField(), idx, endIdx));
                }
                idx += 1;
            }
        }

        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort matches by start position, then by length desc, then priority desc
        matches.sort((m1, m2) -> {
            int comp = Integer.compare(m1.start, m2.start);
            if (comp != 0) {
                return comp;
            }
            int lenComp = Integer.compare(m2.length, m1.length);
            if (lenComp != 0) {
                return lenComp;
            }
            return Integer.compare(m2.field.getPriority(), m1.field.getPriority());
        });

        // Filter out overlapping matches (keep the first/longest match)
        List<LabelMatch> nonOverlapping = new ArrayList<>();
        int lastEnd = -1;
        for (LabelMatch match : matches) {
            if (match.start >= lastEnd) {
                nonOverlapping.add(match);
                lastEnd = match.end;
            }
        }

        // Extract values between consecutive labels
        List<ParsedSegment> segments = new ArrayList<>();
        for (int i = 0; i < nonOverlapping.size(); i++) {
            LabelMatch current = nonOverlapping.get(i);
            int valueStart = current.end;
            int valueEnd = (i + 1 < nonOverlapping.size()) ? nonOverlapping.get(i + 1).start : text.length();

            if (valueStart <= valueEnd && valueStart < text.length()) {
                String rawVal = text.substring(valueStart, valueEnd);
                String cleanVal = cleanValue(rawVal);
                if (!cleanVal.isBlank()) {
                    segments.add(new ParsedSegment(current.field, cleanVal));
                }
            }
        }

        return segments;
    }
}