package com.llmsearch.server.index;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class DocumentStore {
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<Path> paths = new ArrayList<>();

    public int register(Path path) {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        lock.lock();
        try {
            paths.add(path);
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
}
