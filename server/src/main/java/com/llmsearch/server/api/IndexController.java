package com.llmsearch.server.api;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.llmsearch.server.index.IndexStats;
import com.llmsearch.server.index.IndexingService;

@RestController
public class IndexController {
    public record IndexRequest(int threads) {}
    public record IndexResponse(
        String status,
        @JsonProperty("documents_indexed")
        int documentsIndexed,
        @JsonProperty("time_ms")
        long timeMs
    ) {}
    private final IndexingService indexing;

    public IndexController(IndexingService indexing) {
        this.indexing = indexing;
    }

    @PostMapping("/api/index")
    public IndexResponse index(@RequestBody IndexRequest request) throws IOException, InterruptedException {
        IndexStats s = indexing.buildIndex(request.threads());
        return new IndexResponse("ok", s.documents(), s.buildTimeMs());
    }
}
