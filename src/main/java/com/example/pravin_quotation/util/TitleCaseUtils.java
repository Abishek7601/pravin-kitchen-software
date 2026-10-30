package com.example.pravin_quotation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for title capitalization of customer details.
 * Capitalizes the first letter of each word while preserving existing casing,
 * punctuation, and spacing without forcing every letter to uppercase or lowercase.
 */
public final class TitleCaseUtils {

    // Delimiters: start of string, or one or more whitespace/punctuation characters
    private static final Pattern WORD_START_PATTERN = Pattern.compile("(^|[\\s,.\\-/\\\\()\\[\\]#{}:;+&]+)(\\p{Ll})");

    private TitleCaseUtils() {
        // Prevent instantiation
    }

    /**
     * Capitalizes the first letter of each word in the given string.
     * Preserves internal uppercase/lowercase (e.g., McDonald, Pravin KITCHENS)
     * and intentional punctuation and spacing.
     *
     * @param input the input string
     * @return the title-capitalized string, or the input unchanged if null/empty
     */
    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = WORD_START_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String delimiter = matcher.group(1);
            String firstLetter = matcher.group(2).toUpperCase();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(delimiter + firstLetter));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
