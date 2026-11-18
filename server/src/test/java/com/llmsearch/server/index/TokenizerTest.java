package com.llmsearch.server.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TokenizerTest {
    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void lowercasesAndSplitWords() {
        assertEquals(
            List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"),
            tokenizer.tokenize("The quick brown fox jumps over the lazy dog.")
        );
    }

    @Test
    void splitsOnApostrophe(){
        assertEquals(List.of("it", "s"), tokenizer.tokenize("it's"));
    }

    @Test
    void returnsEveryTokenLowercased() {
        assertEquals(List.of("hello", "hello"), tokenizer.tokenize("Hello, HELLO"));
    }

    @Test
    void keepsNonAsciiLetters() {
        assertEquals(List.of("caf\u00E9", "2026"), tokenizer.tokenize("Caf\u00E9 \u00A9 2026"));
    }

    @Test
    void splitsOnParagraphBreaks() {
        assertEquals(List.of("one", "two"), tokenizer.tokenize("one\n\ntwo"));
    }

    @Test
    void emptyInputGivesEmptyList() {
        assertEquals(List.of(), tokenizer.tokenize(""));
        assertEquals(List.of(), tokenizer.tokenize("   \n\t "));
        assertEquals(List.of(), tokenizer.tokenize(null));
    }
}
