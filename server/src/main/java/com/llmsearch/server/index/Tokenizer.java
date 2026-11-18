package com.llmsearch.server.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Tokenizer {
    public List<String> tokenize(String text) {
        if (text == null) {
            return List.of();
        }

        String[] parts = text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"); // split on all non unicode letters and numbers
        List<String> tokens = new ArrayList<>(parts.length);

        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }

        return tokens;
    }
}
