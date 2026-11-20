package com.llmsearch.server.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpandedQuery(String original, @JsonProperty("expanded_terms") List<String> expandedTerms) {}
