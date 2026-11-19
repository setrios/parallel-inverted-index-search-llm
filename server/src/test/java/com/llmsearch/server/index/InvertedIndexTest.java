package com.llmsearch.server.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.llmsearch.server.concurrent.ThreadPool;

public class InvertedIndexTest {
    private final InvertedIndex index = new InvertedIndex(4);

    @Test
    public void returnsAddedPostingsInOrder() {
        index.addPosting("apple", 1, 2);
        index.addPosting("apple", 3, 1);

        assertEquals(List.of(new Posting(1, 2), new Posting(3, 1)), index.get("apple"));
    }

    @Test
    public void unknownTermReturnsEmptyList() {
        assertTrue(index.get("zzz").isEmpty());
    }

    @Test
    public void returnedListIsACopy() {
        index.addPosting("cp", 4, 2);
        List<Posting> list = index.get("cp");
        list.clear();

        assertEquals(List.of(new Posting(4, 2)), index.get("cp"));
    }

    @Test
    public void sizeCountsDistinctTerms() {
        index.addPosting("apple", 1, 2);
        index.addPosting("apple", 3, 1);
        index.addPosting("banana", 1, 1);

        assertEquals(2, index.size());
    }

    @Test
    public void rejectsInvalidSegmentCount() {
        assertThrows(IllegalArgumentException.class, () -> new InvertedIndex(0));
    }

    @Test
    @Timeout(10)
    public void parallelWritesLoseNothing() throws InterruptedException {
        final int docs = 1000;
        final int terms = 100;
        ThreadPool pool = new ThreadPool(8);

        // each task simulates indexing one document
        for (int d = 0; d < docs; d++) {
            final int docId = d; // final to use inside Runnable
            pool.submit(new Runnable() {
                public void run() {
                    for (int t = 0; t < terms; t++) {
                        index.addPosting("term" + t, docId, 1);
                    }
                }
            });
        }
        pool.awaitCompletion();
        pool.shutdown();

        assertEquals(terms, index.size());
        for (int t = 0; t < terms; t++) {
            List<Posting> postings = index.get("term" + t);
            assertEquals(docs, postings.size());

            Set<Integer> docIds = new HashSet<>();
            for (Posting p : postings) {
                docIds.add(p.docId());
            }
            assertEquals(docs, docIds.size()); // no document is missing or duplicated
        }
    }
}
