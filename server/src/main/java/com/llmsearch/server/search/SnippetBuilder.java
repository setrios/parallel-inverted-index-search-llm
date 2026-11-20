package com.llmsearch.server.search;

import java.util.List;
import java.util.Locale;

public class SnippetBuilder {
    private static final int BEFORE = 60; // chars before word
    private static final int AFTER = 140; // chars after word

    // finds word as whole word, "cat" in "a cat", not in "concatenate"
    private int findWord(String text, String word) {
        int from = 0;
        while (true) {
            int i = text.indexOf(word, from);
            if (i < 0) {
                return -1;
            }
            int end = i + word.length();

            // check chars aroud the word
            boolean startOk = i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1));
            boolean endOk = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));

            if (startOk && endOk) {
                return i;
            }
            from = i + 1;
        }
    }

    public String build(String text, List<String> terms) {
        // find where the first query word (from the terms) appears in text, use start of text if none
        String lower = text.toLowerCase(Locale.ROOT);
        int pos = -1;
        for (String term : terms) {
            int i = findWord(lower, term);
            if (i >= 0 && (pos < 0 || i < pos)) {
                pos = i;
            }
        }
        if (pos < 0) {
            pos = 0;
        }

        // clip sides so we are not outside of document
        int start = Math.max(0, pos - BEFORE);
        int end = Math.min(text.length(), pos + AFTER);

        // shrinking inreval so we do not cut words in half
        if (start > 0) {
            int space = text.indexOf(' ', start);
            if (space >= 0 && space < pos) {
                start = space + 1;
            }
        }
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space > pos) {
                end = space;
            }
        }

        // replace any \n or \t with space, so one line of text
        String snippet = text.substring(start, end).replaceAll("\\s+", " ").trim();

        // mark cut edges
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < text.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }
}
