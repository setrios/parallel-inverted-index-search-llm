package com.llmsearch.server.index;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class InvertedIndex {
    private static class Segment {
        final ReentrantLock lock = new ReentrantLock();
        final HashMap<String, ArrayList<Posting>> map = new HashMap<>();
    }
    private final Segment[] segments;

    public InvertedIndex(int segmentCount) {
        if (segmentCount < 1) {
            throw new IllegalArgumentException("segmentCount must be >= 1");
        }

        segments = new Segment[segmentCount];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment();
        }
    }

    private Segment segmentFor(String term) {
        return segments[Math.floorMod(term.hashCode(), segments.length)];
    }

    public void addPosting(String term, int docId, int frequency) {
        Segment segment = segmentFor(term);
        segment.lock.lock();
        try {
            ArrayList<Posting> list = segment.map.get(term);
            if (list == null) {
                list = new ArrayList<>();
                segment.map.put(term, list);
            }
            list.add(new Posting(docId, frequency)); // one posting per term per document (caller passes already counted freq)
        } finally {
            segment.lock.unlock();
        }
    }

    public List<Posting> get(String term) {
        Segment segment = segmentFor(term);
        segment.lock.lock();
        try {
            ArrayList<Posting> list = segment.map.get(term);
            if (list == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(list);
        } finally {
            segment.lock.unlock();
        }
    }

    public int size() {
        int total = 0;
        for (Segment segment : segments) {
            segment.lock.lock();
            try {
                total += segment.map.size();
            } finally {
                segment.lock.unlock();
            }
        }
        return total;
    }
}
