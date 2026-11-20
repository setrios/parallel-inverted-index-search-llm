package com.llmsearch.server.search;

import com.llmsearch.server.index.DocumentStore;
import com.llmsearch.server.index.IndexSnapshot;
import com.llmsearch.server.index.IndexingService;
import com.llmsearch.server.index.InvertedIndex;
import com.llmsearch.server.index.Tokenizer;
import com.llmsearch.server.ollama.QueryExpander;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchService {

    private static final double ORIGINAL_WEIGHT = 1.0;
    private static final double EXPANDED_WEIGHT = 0.5;
    private static final int MAX_RESULTS = 10;

    private final IndexingService indexing;
    private final QueryExpander expander;
    private final Tokenizer tokenizer = new Tokenizer();
    private final Ranker ranker = new Ranker();
    private final SnippetBuilder snippetBuilder = new SnippetBuilder();

    public SearchService(IndexingService indexing, QueryExpander expander) {
        this.indexing = indexing;
        this.expander = expander;
    }

    public ExpandedQuery expandQuery(String query) {
        return expander.expand(query);
    }

    public SearchResponse search(String query) throws IOException {
        long start = System.nanoTime();
        ExpandedQuery q = expandQuery(query);
        List<SearchResult> results = keywordSearch(q);
        long ms = (System.nanoTime() - start) / 1000000;
        return new SearchResponse(results, ms);
    }

    private void addScores(
        InvertedIndex index,
        Set<String> terms,
        double weight,
        int totalDocs,
        HashMap<Integer, Double> totals
    ) {
        for (String term : terms) {
            Map<Integer, Double> scores = ranker.score(
                index.get(term),
                weight,
                totalDocs
            );
            for (Map.Entry<Integer, Double> e : scores.entrySet()) {
                totals.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
    }

    public List<SearchResult> keywordSearch(ExpandedQuery q) throws IOException {
        // one snapshot for the whole search, even if a new index is built at the same tim
        IndexSnapshot snap = indexing.snapshot();
        InvertedIndex index = snap.index();
        DocumentStore store = snap.store();
        int totalDocs = snap.stats().documents();
        if (totalDocs == 0) {
            return List.of();
        }

        // expanded terms that repeat the original ones are dropped
        Set<String> original = new LinkedHashSet<>(tokenizer.tokenize(q.original()));
        Set<String> expanded = new LinkedHashSet<>(tokenizer.tokenize(String.join(" ", q.expandedTerms())));
        expanded.removeAll(original);

        // total score of every found document
        HashMap<Integer, Double> totals = new HashMap<>();
        addScores(index, original, ORIGINAL_WEIGHT, totalDocs, totals);
        addScores(index, expanded, EXPANDED_WEIGHT, totalDocs, totals);
        if (totals.isEmpty()) {
            return List.of();
        }

        // sort found documents by their score - best first | equal scores ordered by docId
        ArrayList<Map.Entry<Integer, Double>> entries = new ArrayList<>(totals.entrySet());
        entries.sort(new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> a, Map.Entry<Integer, Double> b) {
                int byScore = Double.compare(b.getValue(), a.getValue());
                if (byScore != 0) {
                    return byScore;
                }
                return Integer.compare(a.getKey(), b.getKey());
            }
        });

        // take the 10 best documents, scale scores to 0..1, add file name and snippet
        int count = Math.min(MAX_RESULTS, entries.size());
        double max = entries.get(0).getValue();
        List<String> allTerms = new ArrayList<>(original);
        allTerms.addAll(expanded);

        ArrayList<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int docId = entries.get(i).getKey();
            double score = entries.get(i).getValue() / max;
            String text = Files.readString(store.path(docId));
            results.add(new SearchResult(store.name(docId), score, snippetBuilder.build(text, allTerms)));
        }
        return results;
    }
}
