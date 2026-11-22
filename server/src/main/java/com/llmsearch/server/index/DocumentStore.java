package com.llmsearch.server.index;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class DocumentStore {
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<Path> paths = new ArrayList<>();
    private final ArrayList<Integer> lengths = new ArrayList<>(); // number of words, by docId

    public int register(Path path) {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        lock.lock();
        try {
            paths.add(path);
            lengths.add(0); // set later, when the document is read
            return paths.size() - 1;
        } finally {
            lock.unlock();
        }
    }

    public Path path(int docId) {
        lock.lock();
        try {
            if (docId < 0 || docId >= paths.size()) {
                throw new IllegalArgumentException("unknown docId");
            }
            return paths.get(docId);
        } finally {
            lock.unlock();
        }
    }

    public String name(int docId) {
        return path(docId).getFileName().toString();
    }

    public int count() {
        lock.lock();
        try {
            return paths.size();
        } finally {
            lock.unlock();
        }
    }

    public void setLength(int docId, int length) {
        lock.lock();
        try {
            if (docId < 0 || docId >= lengths.size()) {
                throw new IllegalArgumentException("unknown docId");
            }
            lengths.set(docId, length);
        } finally {
            lock.unlock();
        }
    }

    // a copy, so the caller can use it without the lock
    public int[] lengths() {
        lock.lock();
        try {
            int[] copy = new int[lengths.size()];
            for (int i = 0; i < copy.length; i++) {
                copy[i] = lengths.get(i);
            }
            return copy;
        } finally {
            lock.unlock();
        }
    }
}
