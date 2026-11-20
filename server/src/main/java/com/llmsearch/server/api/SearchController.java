package com.llmsearch.server.api;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.llmsearch.server.search.SearchResponse;
import com.llmsearch.server.search.SearchService;

@RestController
public class SearchController {
    public record SearchRequest(String query, String mode) {}
    private final SearchService search;
    public SearchController(SearchService search) {
        this.search = search;
    }

    @PostMapping("/api/search")
    public SearchResponse search(@RequestBody SearchRequest request) throws IOException {
        if (request.mode() != null && !"keyword".equals(request.mode())) {
            throw new IllegalArgumentException("unsupported mode: " + request.mode());
        }
        return search.search(request.query());
    }
}
