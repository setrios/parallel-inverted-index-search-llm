package com.llmsearch.server.index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.llmsearch.server.concurrent.ThreadPool;

public class DocumentStoreTest {
    private final DocumentStore store = new DocumentStore();

    @Test
    public void registerGivesSequentialIds() {
        assertEquals(0, store.register(Path.of("data/doc_00001.txt")));
        assertEquals(1, store.register(Path.of("data/doc_00002.txt")));
        assertEquals(2, store.register(Path.of("data/doc_00003.txt")));
    }

    @Test
    public void returnsPathAndNameById() {
        store.register(Path.of("data/doc_00001.txt"));
        int id = store.register(Path.of("data/doc_00002.txt"));

        assertEquals(Path.of("data/doc_00002.txt"), store.path(id));
        assertEquals("doc_00002.txt", store.name(id));
    }

    @Test
    public void countsRegisteredDocuments() {
        assertEquals(0, store.count());

        store.register(Path.of("a.txt"));
        store.register(Path.of("b.txt"));
        store.register(Path.of("c.txt"));

        assertEquals(3, store.count());
    }

    @Test
    public void unknownDocIdIsRejected() {
        store.register(Path.of("a.txt"));

        assertThrows(IllegalArgumentException.class, () -> store.path(1));
        assertThrows(IllegalArgumentException.class, () -> store.path(-1));
    }

    @Test
    public void nullPathIsRejected() {
        assertThrows(NullPointerException.class, () -> store.register(null));
    }

    @Test
    public void savesLengthsOfDocuments() {
        store.register(Path.of("a.txt"));
        store.register(Path.of("b.txt"));

        store.setLength(1, 50);

        assertArrayEquals(new int[] {0, 50}, store.lengths()); // a.txt is not set yet
    }

    @Test
    public void lengthsAreACopy() {
        store.register(Path.of("a.txt"));
        store.setLength(0, 7);

        int[] lengths = store.lengths();
        lengths[0] = 100;

        assertArrayEquals(new int[] {7}, store.lengths());
    }

    @Test
    public void lengthOfUnknownIdThrows() {
        store.register(Path.of("a.txt"));

        assertThrows(IllegalArgumentException.class, () -> store.setLength(1, 5));
        assertThrows(IllegalArgumentException.class, () -> store.setLength(-1, 5));
    }

    @Test
    @Timeout(10)
    public void parallelRegistrationGivesUniqueIds() throws InterruptedException {
        final int docs = 5000;
        ThreadPool pool = new ThreadPool(8);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < docs; i++) {
            final int n = i; // each task needs its own unchanging copy of the loop variable
            pool.submit(new Runnable() {
                public void run() {
                    ids.add(store.register(Path.of("file" + n)));
                }
            });
        }
        pool.awaitCompletion();
        pool.shutdown();

        assertEquals(docs, store.count());
        assertEquals(docs, ids.size()); // no two threads got the same docId
    }
}
