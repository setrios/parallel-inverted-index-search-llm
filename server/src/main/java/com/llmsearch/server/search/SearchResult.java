package com.llmsearch.server.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchResult(@JsonProperty("doc_id") String docId, double score, String snippet) {}
