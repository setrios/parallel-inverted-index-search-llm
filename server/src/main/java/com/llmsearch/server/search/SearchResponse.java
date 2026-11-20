package com.llmsearch.server.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchResponse(List<SearchResult> results, @JsonProperty("time_ms") long timeMs) {}
