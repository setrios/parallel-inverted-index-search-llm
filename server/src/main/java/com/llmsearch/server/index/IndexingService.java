package com.llmsearch.server.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.llmsearch.server.concurrent.ThreadPool;

public class IndexingService {
    private static final int SEGMENTS = 16;
    private static final int MAX_THREADS = 64; // more threads than this only waste memory, and a huge number crashes the server

    private final Path dataDir;
    private final ReentrantLock buildLock = new ReentrantLock();
    private final Tokenizer tokenizer = new Tokenizer();
    private volatile IndexSnapshot snapshot = new IndexSnapshot(
        new InvertedIndex(SEGMENTS),
        new DocumentStore(),
        new IndexStats(0, 0, 0)
    );

    public IndexingService(Path dataDir) {
        this.dataDir = dataDir;
    }

    public IndexStats getStats() {
        return snapshot.stats();
    }

    public IndexSnapshot snapshot() {
        return snapshot;
    }

    private List<Path> listDocuments() throws IOException {
        ArrayList<Path> files = new ArrayList<>();

        try (DirectoryStream<Path> dir = Files.newDirectoryStream(dataDir, "*.txt")) {
            for (Path p : dir) {
                files.add(p);
            }
        }

        Collections.sort(files);
        return files;
    }

    private void indexDocument(InvertedIndex index, DocumentStore store, int docId, Path file) throws IOException {
        String text = Files.readString(file); // reads whole files into a string
        List<String> terms = tokenizer.tokenize(text);
        store.setLength(docId, terms.size()); // BM25 needs the length of every document

        HashMap<String, Integer> counts = new HashMap<>();
        for (String term : terms) {
            counts.merge(term, 1, Integer::sum); // +1 if entry exists
        }

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            index.addPosting(e.getKey(), docId, e.getValue());
        }
    }

    public IndexStats buildIndex(int threads) throws IOException, InterruptedException {
        if (threads < 1 || threads > MAX_THREADS) {
            throw new IllegalArgumentException("number of threads must be from 1 to " + MAX_THREADS);
        }

        // each HTTP request runs in its own web server thread, so two requests
        // can call buildIndex at the same time. only one build must run at once
        if (!buildLock.tryLock()) {
            throw new IndexingInProgressException();
        }

        try {
            long start = System.nanoTime();
            InvertedIndex newIndex = new InvertedIndex(SEGMENTS);
            DocumentStore newStore = new DocumentStore();
            List<Path> files = listDocuments();

            ThreadPool pool = new ThreadPool(threads);
            try {
                for (Path file : files) {
                    final int docId = newStore.register(file);
                    pool.submit(new Runnable() {
                        public void run() {
                            try {
                                indexDocument(newIndex, newStore, docId, file);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    });
                }
                pool.awaitCompletion();
                if (pool.failedTasks() > 0) {
                    throw new IllegalStateException(pool.failedTasks() + " documents failed to index, see the server log");
                }
            } finally {
                pool.shutdown();
            }
            long ms = (System.nanoTime() - start) / 1000000;
            IndexStats stats = new IndexStats(newStore.count(), ms, threads);
            snapshot = new IndexSnapshot(newIndex, newStore, stats);
            return stats;
        } finally {
            buildLock.unlock();
        }
    }
}
