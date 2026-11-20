package com.llmsearch.server.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.llmsearch.server.index.IndexStats;
import com.llmsearch.server.index.IndexingService;

@RestController
public class StatsController {
    public record StatsResponse(
        int documents,
        @JsonProperty("index_build_time_ms")
        long indexBuildTimeMs,
        @JsonProperty("threads_used")
        int threadsUsed
    ) {}

    private final IndexingService indexing;

    public StatsController(IndexingService indexing) {
        this.indexing = indexing;
    }

    @GetMapping("/api/stats")
    public StatsResponse stats() {
        IndexStats s = indexing.getStats();
        return new StatsResponse(s.documents(), s.buildTimeMs(), s.threadsUsed());
    }
}
