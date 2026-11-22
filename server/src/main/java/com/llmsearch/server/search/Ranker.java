package com.llmsearch.server.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.llmsearch.server.index.Posting;

// BM25: like TF-IDF, but repeats of a word give less and less score
// and long documents do not win just because they have more words
public class Ranker {
    private static final double K1 = 1.2;  // how fast repeats of a word stop adding score
    private static final double B = 0.75;  // how much the document length matters, 0 = not at all

    private final int[] docLengths; // number of words in every document, by docId
    private final double avgLength;

    public Ranker(int[] docLengths) {
        this.docLengths = docLengths;

        long sum = 0;
        for (int length : docLengths) {
            sum += length;
        }
        this.avgLength = docLengths.length == 0 ? 0 : (double) sum / docLengths.length;
    }

    // scores each document containing the term
    public Map<Integer, Double> score(List<Posting> postings, double weight) {
        HashMap<Integer, Double> scores = new HashMap<>();

        if (postings.isEmpty()) {
            return scores;
        }

        int totalDocs = docLengths.length;
        int df = postings.size(); // number of documents with the term
        double idf = Math.log(1.0 + (totalDocs - df + 0.5) / (df + 0.5));

        for (Posting p : postings) {
            double tf = p.frequency();
            // 1 for a document of average length, more for longer ones, less for shorter ones
            double lengthNorm = 1 - B + B * docLengths[p.docId()] / avgLength;
            scores.put(p.docId(), weight * idf * tf * (K1 + 1) / (tf + K1 * lengthNorm));
        }

        return scores;
    }
}
