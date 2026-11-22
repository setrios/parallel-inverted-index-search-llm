package com.llmsearch.server.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.llmsearch.server.index.Posting;

public class RankerTest {
    private static final double DELTA = 0.0001; // allowed error when comparing doubles

    @Test
    public void scoresByBm25() {
        // 4 documents of the same length, so the length part is 1 and the score is idf * tf * 2.2 / (tf + 1.2)
        // the term is in 2 of them: idf = log(1 + (4 - 2 + 0.5) / (2 + 0.5)) = log(2)
        Ranker ranker = new Ranker(new int[] {10, 10, 10, 10});
        Map<Integer, Double> scores = ranker.score(List.of(new Posting(0, 3), new Posting(2, 1)), 1.0);

        assertEquals(2, scores.size());
        assertEquals(Math.log(2) * 3 * 2.2 / (3 + 1.2), scores.get(0), DELTA);
        assertEquals(Math.log(2) * 1 * 2.2 / (1 + 1.2), scores.get(2), DELTA);
    }

    @Test
    public void weightScalesTheScore() {
        Ranker ranker = new Ranker(new int[] {10, 10, 10});
        List<Posting> postings = List.of(new Posting(0, 3));

        double full = ranker.score(postings, 1.0).get(0);
        double half = ranker.score(postings, 0.5).get(0);

        assertEquals(full / 2, half, DELTA);
    }

    @Test
    public void rarerTermScoresHigher() {
        // the same frequency, but the rare term is in 1 document and the common one in all 4
        Ranker ranker = new Ranker(new int[] {10, 10, 10, 10});
        double rare = ranker.score(List.of(new Posting(0, 2)), 1.0).get(0);
        double common = ranker.score(List.of(new Posting(0, 2), new Posting(1, 2), new Posting(2, 2), new Posting(3, 2)), 1.0).get(0);

        assertTrue(rare > common);
    }

    @Test
    public void termInEveryDocumentStillScoresAboveZero() {
        Ranker ranker = new Ranker(new int[] {10, 10});
        double score = ranker.score(List.of(new Posting(0, 1), new Posting(1, 1)), 1.0).get(0);

        // idf = log(1 + 0.5 / 2.5) = log(1.2)
        assertEquals(Math.log(1.2), score, DELTA);
    }

    @Test
    public void repeatsAddLessAndLess() {
        // 1000 repeats of a word must not give 100 times more than 10 repeats
        Ranker ranker = new Ranker(new int[] {10, 10, 10});
        double ten = ranker.score(List.of(new Posting(0, 10)), 1.0).get(0);
        double thousand = ranker.score(List.of(new Posting(0, 1000)), 1.0).get(0);

        assertTrue(thousand > ten);
        assertTrue(thousand < ten * 1.2);
    }

    @Test
    public void shorterDocumentScoresHigher() {
        // the word is 5 times in both documents, but document 0 is 10 times shorter
        Ranker ranker = new Ranker(new int[] {100, 1000, 500});
        Map<Integer, Double> scores = ranker.score(List.of(new Posting(0, 5), new Posting(1, 5)), 1.0);

        assertTrue(scores.get(0) > scores.get(1));
    }

    @Test
    public void averageLengthIsNotRoundedDown() {
        // average of 1 and 2 is 1.5 | integer division would give 1
        // length part of document 0: 0.25 + 0.75 * 1 / 1.5 = 0.75
        Ranker ranker = new Ranker(new int[] {1, 2});
        double score = ranker.score(List.of(new Posting(0, 1)), 1.0).get(0);

        // idf = log(1 + 1.5 / 1.5) = log(2)
        assertEquals(Math.log(2) * 2.2 / (1 + 1.2 * 0.75), score, DELTA);
    }

    @Test
    public void noPostingsGiveNoScores() {
        Ranker ranker = new Ranker(new int[] {10});

        assertTrue(ranker.score(List.of(), 1.0).isEmpty());
    }
}
