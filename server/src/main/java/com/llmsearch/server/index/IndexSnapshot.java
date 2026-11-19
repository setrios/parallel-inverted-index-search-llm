package com.llmsearch.server.index;

public record IndexSnapshot(InvertedIndex index, DocumentStore store, IndexStats stats) {}
