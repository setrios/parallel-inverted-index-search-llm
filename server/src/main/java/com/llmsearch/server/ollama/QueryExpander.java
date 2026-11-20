package com.llmsearch.server.ollama;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

import com.llmsearch.server.search.ExpandedQuery;

public class QueryExpander {
    private static final Logger log = LoggerFactory.getLogger(QueryExpander.class);
    private static final int MAX_TERMS = 5;
    private static final int MAX_WORDS_PER_TERM = 3;

    private final OllamaClient ollama;

    public QueryExpander(OllamaClient ollama) {
        this.ollama = ollama;
    }

    public ExpandedQuery expand(String query) {
        if (query == null || query.isBlank()) {
            return new ExpandedQuery(query, List.of());
        }
        try {
            List<String> terms = parse(ollama.generate(prompt(query)));
            log.info("expanded '{}' -> {}", query, terms);
            return new ExpandedQuery(query, terms);
        } catch (RestClientException e) {
            // ollama is down or too slow: search continues without expansion
            log.warn("query expansion skipped: {}", e.getMessage());
            return new ExpandedQuery(query, List.of());
        }
    }

    private String prompt(String query) {
        return "Give up to " + MAX_TERMS + " English synonyms or closely related keywords for the search query: \""
            + query + "\". Answer with only the words, separated by commas.";
    }

    // cleanup llm response as it may vary 
    List<String> parse(String answer) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (answer == null) {
            return new ArrayList<>(terms);
        }
        for (String part : answer.split("[,:\\n]")) {
            String term = part.trim()
                .replaceAll("^\\d+[.)]\\s*", "") // cleanup if got list: "1. car" -> "car"
                .replaceAll("[\"'*.]", "")       // cleanup quotes, markdown stars, dots
                .trim()
                .toLowerCase(Locale.ROOT);
            if (!term.isEmpty() && term.split("\\s+").length <= MAX_WORDS_PER_TERM) {
                terms.add(term);
            }
        }
        List<String> list = new ArrayList<>(terms);
        return list.size() > MAX_TERMS ? list.subList(0, MAX_TERMS) : list;
    }
}