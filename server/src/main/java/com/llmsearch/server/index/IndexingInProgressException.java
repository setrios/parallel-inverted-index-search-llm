package com.llmsearch.server.index;

public class IndexingInProgressException extends RuntimeException {
    public IndexingInProgressException() {
        super("indexing is already in progress");
    }
}
