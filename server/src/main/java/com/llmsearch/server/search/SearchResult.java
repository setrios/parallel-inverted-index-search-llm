package com.llmsearch.server.search;

public record SearchResult(String docId, double score, String snippet) {}
