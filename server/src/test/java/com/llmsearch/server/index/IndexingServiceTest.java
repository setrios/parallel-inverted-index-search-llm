package com.llmsearch.server.index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

public class IndexingServiceTest {
    @TempDir
    Path dir; // JUnit gives every test own empty temp folder and deletes it after

    private void write(String name, String text) throws IOException {
        Files.writeString(dir.resolve(name), text);
    }

    private Set<Posting> postings(InvertedIndex index, String term) {
        return new HashSet<>(index.get(term)); // as a set: the order of postings depends on thread timing
    }

    @Test
    public void statsAreZeroBeforeFirstBuild() {
        IndexingService service = new IndexingService(dir);

        assertEquals(new IndexStats(0, 0, 0), service.getStats());
    }

    @Test
    @Timeout(10)
    public void buildsIndexFromFiles() throws IOException, InterruptedException {
        write("a.txt", "cat dog cat");
        write("b.txt", "dog bird");
        IndexingService service = new IndexingService(dir);

        IndexStats stats = service.buildIndex(2);

        assertEquals(2, stats.documents());
        assertEquals(2, stats.threadsUsed());
        assertEquals(stats, service.getStats());

        InvertedIndex index = service.snapshot().index();
        assertEquals(Set.of(new Posting(0, 2)), postings(index, "cat"));
        assertEquals(Set.of(new Posting(0, 1), new Posting(1, 1)), postings(index, "dog"));
        assertEquals(Set.of(new Posting(1, 1)), postings(index, "bird"));
    }

    @Test
    @Timeout(10)
    public void savesNumberOfWordsOfEveryDocument() throws IOException, InterruptedException {
        write("a.txt", "cat dog cat");
        write("b.txt", "dog, bird!");
        IndexingService service = new IndexingService(dir);

        service.buildIndex(2);

        assertArrayEquals(new int[] {3, 2}, service.snapshot().store().lengths());
    }

    @Test
    @Timeout(10)
    public void ignoresFilesWithOtherExtensions() throws IOException, InterruptedException {
        write("a.txt", "cat");
        write("notes.log", "cat dog");
        IndexingService service = new IndexingService(dir);

        IndexStats stats = service.buildIndex(2);

        assertEquals(1, stats.documents());
        assertEquals(0, service.snapshot().index().get("dog").size());
    }

    @Test
    @Timeout(10)
    public void docIdsFollowSortedFileNames() throws IOException, InterruptedException {
        for (int i = 19; i >= 0; i--) { // created in reverse
            write(String.format("f%02d.txt", i), "word");
        }
        IndexingService service = new IndexingService(dir);

        service.buildIndex(4);

        DocumentStore store = service.snapshot().store();
        for (int i = 0; i < 20; i++) {
            assertEquals(String.format("f%02d.txt", i), store.name(i));
        }
    }

    @Test
    @Timeout(20)
    public void sameIndexForAnyNumberOfThreads() throws IOException, InterruptedException {
        Random random = new Random(1);
        for (int i = 0; i < 100; i++) {
            StringBuilder text = new StringBuilder();
            for (int j = 0; j < 300; j++) {
                text.append("w").append(random.nextInt(50)).append(' ');
            }
            write(String.format("doc%03d.txt", i), text.toString());
        }
        IndexingService service = new IndexingService(dir);

        service.buildIndex(1);
        InvertedIndex single = service.snapshot().index();
        service.buildIndex(8);
        InvertedIndex parallel = service.snapshot().index();

        assertEquals(single.size(), parallel.size());
        for (int t = 0; t < 50; t++) {
            String term = "w" + t;
            assertEquals(postings(single, term), postings(parallel, term));
        }
    }

    @Test
    @Timeout(10)
    public void secondBuildReplacesTheIndex() throws IOException, InterruptedException {
        write("a.txt", "cat");
        IndexingService service = new IndexingService(dir);
        service.buildIndex(1);
        InvertedIndex first = service.snapshot().index();

        write("b.txt", "dog");
        IndexStats stats = service.buildIndex(1);

        assertEquals(2, stats.documents());
        assertEquals(2, service.getStats().documents());
        assertEquals(1, service.snapshot().index().get("dog").size());
        assertEquals(0, first.get("dog").size()); // the old index is left untouched
    }

    @Test
    public void rejectsInvalidThreadCount() {
        IndexingService service = new IndexingService(dir);

        assertThrows(IllegalArgumentException.class, () -> service.buildIndex(0));
        assertThrows(IllegalArgumentException.class, () -> service.buildIndex(-3));
    }

    @Test
    @Timeout(10)
    public void rejectsTooManyThreads() throws IOException, InterruptedException {
        write("a.txt", "cat");
        IndexingService service = new IndexingService(dir);

        assertThrows(IllegalArgumentException.class, () -> service.buildIndex(65));
        assertThrows(IllegalArgumentException.class, () -> service.buildIndex(1000000));
        assertEquals(1, service.buildIndex(64).documents()); // 64 is still allowed
    }

    @Test
    @Timeout(10)
    public void unreadableDocumentFailsTheBuild() throws IOException, InterruptedException {
        write("a.txt", "cat");
        IndexingService service = new IndexingService(dir);
        service.buildIndex(2);

        // a folder named like a document: it is listed, but reading it as a file fails
        Files.createDirectory(dir.resolve("broken.txt"));

        assertThrows(IllegalStateException.class, () -> service.buildIndex(2));
        assertEquals(1, service.getStats().documents()); // the previous index is kept
    }

    @Test
    @Timeout(10)
    public void failedBuildKeepsOldStateAndReleasesLock() throws InterruptedException {
        IndexingService service = new IndexingService(dir.resolve("missing"));

        assertThrows(IOException.class, () -> service.buildIndex(2));
        assertEquals(new IndexStats(0, 0, 0), service.getStats());

        // ReentrantLock lets the same thread lock again, so use another thread
        final Throwable[] error = new Throwable[1]; // array lets the thread pass its exception back to the test
        Thread other = new Thread(new Runnable() {
            public void run() {
                try {
                    service.buildIndex(2);
                } catch (Throwable e) {
                    error[0] = e;
                }
            }
        });
        other.start();
        other.join();

        assertTrue(error[0] instanceof IOException);
    }
}
