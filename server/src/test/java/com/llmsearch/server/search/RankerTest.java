package com.llmsearch.server.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.llmsearch.server.index.Posting;

public class RankerTest {
    private static final double DELTA = 0.0001; // allowed error when comparing doubles

    private final Ranker ranker = new Ranker();

    @Test
    public void scoresByTfIdf() {
        // the term is in 2 of 4 documents: idf = log(1 + 4/2) = log(3)
        Map<Integer, Double> scores = ranker.score(List.of(new Posting(0, 3), new Posting(2, 1)), 1.0, 4);

        assertEquals(2, scores.size());
        assertEquals(3 * Math.log(3), scores.get(0), DELTA);
        assertEquals(1 * Math.log(3), scores.get(2), DELTA);
    }

    @Test
    public void weightScalesTheScore() {
        List<Posting> postings = List.of(new Posting(0, 3));

        double full = ranker.score(postings, 1.0, 10).get(0);
        double half = ranker.score(postings, 0.5, 10).get(0);

        assertEquals(full / 2, half, DELTA);
    }

    @Test
    public void rarerTermScoresHigher() {
        // the same frequency, but the rare term is in 1 document and the common one in all 4
        double rare = ranker.score(List.of(new Posting(0, 2)), 1.0, 4).get(0);
        double common = ranker.score(List.of(new Posting(0, 2), new Posting(1, 2), new Posting(2, 2), new Posting(3, 2)), 1.0, 4).get(0);

        assertTrue(rare > common);
    }

    @Test
    public void termInEveryDocumentStillScoresAboveZero() {
        double score = ranker.score(List.of(new Posting(0, 1), new Posting(1, 1)), 1.0, 2).get(0);

        assertEquals(Math.log(2), score, DELTA);
    }

    @Test
    public void ratioIsNotRoundedDown() {
        // 3 documents / 2 = 1.5 | integer division would give 1
        double score = ranker.score(List.of(new Posting(0, 1), new Posting(1, 1)), 1.0, 3).get(0);

        assertEquals(Math.log(2.5), score, DELTA);
    }

    @Test
    public void noPostingsGiveNoScores() {
        assertTrue(ranker.score(List.of(), 1.0, 10).isEmpty());
    }
}
