package com.llmsearch.server.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.llmsearch.server.search.ExpandedQuery;

public class QueryExpanderTest {
    // nothing listens on port 1, so every request to this client fails 
    private final QueryExpander expander = new QueryExpander(new OllamaClient("http://localhost:1", "llama3.2:3b"));

    @Test
    public void parsesCommaSeparatedAnswer() {
        assertEquals(List.of("vehicle", "automobile", "motorcar"),
            expander.parse("Vehicle, automobile, motorcar"));
    }

    @Test
    public void parsesNumberedList() {
        assertEquals(List.of("vehicle", "automobile", "motorcar"),
            expander.parse("1. Vehicle\n2. Automobile\n3) Motorcar"));
    }

    @Test
    public void removesQuotesStarsAndDots() {
        assertEquals(List.of("vehicle", "auto", "motorcar"),
            expander.parse("\"vehicle\", 'auto', **motorcar**."));
    }

    @Test
    public void dropsIntroductionBeforeColon() {
        assertEquals(List.of("vehicle", "automobile"),
            expander.parse("Here are some synonyms for car: vehicle, automobile"));
    }

    @Test
    public void keepsShortPhrases() {
        assertEquals(List.of("motor vehicle", "sports utility vehicle"),
            expander.parse("motor vehicle, sports utility vehicle"));
    }

    @Test
    public void dropsRepeatsIgnoringCase() {
        assertEquals(List.of("vehicle"), expander.parse("vehicle, Vehicle, VEHICLE"));
    }

    @Test
    public void keepsAtMostFiveTerms() {
        assertEquals(List.of("a", "b", "c", "d", "e"), expander.parse("a, b, c, d, e, f, g"));
    }

    @Test
    public void emptyOrMissingAnswerGivesNoTerms() {
        assertEquals(List.of(), expander.parse(""));
        assertEquals(List.of(), expander.parse(null));
    }

    @Test
    @Timeout(10)
    public void searchesWithoutExpansionWhenOllamaIsDown() {
        ExpandedQuery q = expander.expand("car");

        assertEquals("car", q.original());
        assertEquals(List.of(), q.expandedTerms());
    }

    @Test
    public void blankQueryGivesNoTerms() {
        assertEquals(List.of(), expander.expand("   ").expandedTerms());
    }
}