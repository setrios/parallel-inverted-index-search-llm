package com.llmsearch.server.search;

import java.util.List;

public record ExpandedQuery(String original, List<String> expandedTerms) {}
