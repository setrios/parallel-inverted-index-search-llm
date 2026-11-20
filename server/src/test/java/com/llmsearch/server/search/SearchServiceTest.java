package com.llmsearch.server.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.llmsearch.server.index.IndexingService;

public class SearchServiceTest {
    private static final double DELTA = 0.0001; // allowed error when comparing doubles

    @TempDir
    Path dir; // JUnit gives every test own empty temp folder and deletes it after

    private void write(String name, String text) throws IOException {
        Files.writeString(dir.resolve(name), text);
    }

    // builds the index over the files in dir and returns a search service for it
    private SearchService searchOverFiles() throws IOException, InterruptedException {
        IndexingService indexing = new IndexingService(dir);
        indexing.buildIndex(2);
        return new SearchService(indexing, null);
    }

    private List<SearchResult> search(SearchService service, String query, String... expanded) throws IOException {
        return service.keywordSearch(new ExpandedQuery(query, List.of(expanded)));
    }

    @Test
    public void noResultsBeforeIndexIsBuilt() throws IOException {
        SearchService service = new SearchService(new IndexingService(dir), null);

        assertEquals(0, search(service, "cat").size());
    }

    @Test
    @Timeout(10)
    public void bestDocumentFirstWithScoreOne() throws IOException, InterruptedException {
        write("a.txt", "cat cat dog");
        write("b.txt", "cat dog");
        write("c.txt", "bird");
        SearchService service = searchOverFiles();

        List<SearchResult> results = search(service, "cat");

        assertEquals(2, results.size());
        assertEquals("a.txt", results.get(0).docId());
        assertEquals(1.0, results.get(0).score(), DELTA);
        assertEquals("b.txt", results.get(1).docId());
        assertEquals(0.5, results.get(1).score(), DELTA);
    }

    @Test
    @Timeout(10)
    public void unknownWordOrEmptyQueryGivesNoResults() throws IOException, InterruptedException {
        write("a.txt", "cat");
        SearchService service = searchOverFiles();

        assertEquals(0, search(service, "zebra").size());
        assertEquals(0, search(service, "").size());
    }

    @Test
    @Timeout(10)
    public void queryIsTokenizedLikeDocuments() throws IOException, InterruptedException {
        write("a.txt", "the cat sat");
        SearchService service = searchOverFiles();

        // upper case and punctuation in query must not matter
        List<SearchResult> results = search(service, "CAT!");

        assertEquals(1, results.size());
        assertEquals("a.txt", results.get(0).docId());
    }

    @Test
    @Timeout(10)
    public void caseAndRepeatsDoNotChangeScores() throws IOException, InterruptedException {
        write("a.txt", "cat cat");
        write("b.txt", "dog dog");
        SearchService service = searchOverFiles();

        // if cat were counted three times, a.txt would beat b.txt
        List<SearchResult> results = search(service, "Cat cat CAT dog");

        assertEquals(2, results.size());
        assertEquals(1.0, results.get(0).score(), DELTA);
        assertEquals(1.0, results.get(1).score(), DELTA);
    }

    @Test
    @Timeout(10)
    public void expandedTermsFindMoreButWeighHalf() throws IOException, InterruptedException {
        write("a.txt", "car car");
        write("b.txt", "automobile automobile");
        write("c.txt", "other");
        SearchService service = searchOverFiles();

        List<SearchResult> results = search(service, "car", "automobile");

        assertEquals(2, results.size());
        assertEquals("a.txt", results.get(0).docId());
        assertEquals(1.0, results.get(0).score(), DELTA);
        assertEquals("b.txt", results.get(1).docId());
        assertEquals(0.5, results.get(1).score(), DELTA);
    }

    @Test
    @Timeout(10)
    public void expandedPhrasesAreSplitIntoWords() throws IOException, InterruptedException {
        write("a.txt", "car");
        write("b.txt", "motor");
        SearchService service = searchOverFiles();

        List<SearchResult> results = search(service, "car", "motor vehicle");

        assertEquals(2, results.size());
        assertEquals("b.txt", results.get(1).docId());
    }

    @Test
    @Timeout(10)
    public void expandedTermRepeatingOriginalIsNotCountedTwice() throws IOException, InterruptedException {
        write("a.txt", "cat");
        write("b.txt", "dog");
        SearchService service = searchOverFiles();

        // "cat" also comes back from expansion; it must still weigh the same as "dog"
        List<SearchResult> results = search(service, "cat dog", "cat");

        assertEquals(2, results.size());
        assertEquals(1.0, results.get(0).score(), DELTA);
        assertEquals(1.0, results.get(1).score(), DELTA);
    }

    @Test
    @Timeout(10)
    public void equalScoresAreOrderedByFileName() throws IOException, InterruptedException {
        write("b.txt", "cat");
        write("a.txt", "cat");
        write("c.txt", "cat");
        SearchService service = searchOverFiles();

        List<SearchResult> results = search(service, "cat");

        assertEquals("a.txt", results.get(0).docId());
        assertEquals("b.txt", results.get(1).docId());
        assertEquals("c.txt", results.get(2).docId());
    }

    @Test
    @Timeout(10)
    public void returnsAtMostTenBestResults() throws IOException, InterruptedException {
        for (int i = 1; i <= 15; i++) {
            write(String.format("doc%02d.txt", i), "word ".repeat(i));
        }
        SearchService service = searchOverFiles();

        List<SearchResult> results = search(service, "word");

        assertEquals(10, results.size());
        assertEquals("doc15.txt", results.get(0).docId());
        assertEquals("doc06.txt", results.get(9).docId());
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).score() > results.get(i).score());
        }
    }

    @Test
    @Timeout(10)
    public void resultHasSnippetAroundTheWord() throws IOException, InterruptedException {
        write("a.txt", "The black cat sat on the mat.");
        SearchService service = searchOverFiles();

        SearchResult result = search(service, "cat").get(0);

        assertEquals("a.txt", result.docId());
        assertEquals("The black cat sat on the mat.", result.snippet());
    }
}
