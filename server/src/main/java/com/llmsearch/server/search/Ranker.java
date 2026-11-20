package com.llmsearch.server.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.llmsearch.server.index.Posting;

public class Ranker {
    // scores each document containing the term: weight * frequency * idf
    public Map<Integer, Double> score(List<Posting> postings, double weight, int totalDocs) {
        HashMap<Integer, Double> scores = new HashMap<>();

        if (postings.isEmpty()) {
            return scores;
        }

        double idf = Math.log(1.0 + (double) totalDocs / postings.size());

        for (Posting p : postings) {
            scores.put(p.docId(), weight * p.frequency() * idf);
        }

        return scores;
    }
}
