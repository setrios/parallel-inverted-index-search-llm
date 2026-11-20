package com.llmsearch.server.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SnippetBuilderTest {
    private final SnippetBuilder builder = new SnippetBuilder();

    @Test
    public void shortTextIsReturnedWhole() {
        assertEquals("The cat sat.", builder.build("The cat sat.", List.of("cat")));
    }

    @Test
    public void longTextIsCutWithDotsAndWholeWords() {
        // word lengths chosen so that both window edges fall inside a word
        String text = "apples ".repeat(50) + "found " + "orange ".repeat(50);

        String snippet = builder.build(text, List.of("found"));

        assertTrue(snippet.startsWith("..."));
        assertTrue(snippet.endsWith("..."));
        assertTrue(snippet.contains("found"));
        assertTrue(snippet.length() < 220);
        // every word is whole, like no "ora" at the edge
        for (String word : snippet.replace("...", "").trim().split(" ")) {
            assertTrue(Set.of("apples", "found", "orange").contains(word), "cut word: " + word);
        }
    }

    @Test
    public void findsWholeWordNotPartOfAnother() {
        String text = "concatenate " + "x ".repeat(150) + "a cat here";

        String snippet = builder.build(text, List.of("cat"));

        assertTrue(snippet.contains("a cat here"));
        assertFalse(snippet.contains("concatenate"));
    }

    @Test
    public void searchIgnoresCase() {
        String text = "x ".repeat(150) + "People here";

        String snippet = builder.build(text, List.of("people"));

        assertTrue(snippet.contains("People here"));
    }

    @Test
    public void usesTheEarliestTermInText() {
        String text = "x ".repeat(150) + "first " + "y ".repeat(150) + "second";

        // "second" is listed first, but "first" appears earlier in the text
        String snippet = builder.build(text, List.of("second", "first"));

        assertTrue(snippet.contains("first"));
        assertFalse(snippet.contains("second"));
    }

    @Test
    public void lineBreaksBecomeSpaces() {
        assertEquals("first line second line", builder.build("first line\n\nsecond line", List.of("second")));
    }

    @Test
    public void noSpacesAtTheEdges() {
        assertEquals("hello world", builder.build("\n\nhello world\n", List.of("hello")));
    }

    @Test
    public void noMatchGivesStartOfText() {
        String text = "x ".repeat(150);

        String snippet = builder.build(text, List.of("missing"));

        assertTrue(snippet.startsWith("x x"));
        assertTrue(snippet.endsWith("..."));
    }
}
