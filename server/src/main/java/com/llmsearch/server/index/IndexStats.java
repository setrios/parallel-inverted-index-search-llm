package com.llmsearch.server.index;

public record IndexStats(int documents, long buildTimeMs, int threadsUsed) {}
