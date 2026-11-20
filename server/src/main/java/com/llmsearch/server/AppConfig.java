package com.llmsearch.server;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.llmsearch.server.index.IndexingService;
import com.llmsearch.server.ollama.OllamaClient;
import com.llmsearch.server.ollama.QueryExpander;
import com.llmsearch.server.search.SearchService;

@Configuration
public class AppConfig {
    @Bean
    public IndexingService indexingService(@Value("${search.data-dir}") String dataDir) {
        return new IndexingService(Path.of(dataDir));
    }

    @Bean
    public SearchService searchService(IndexingService indexingService, QueryExpander queryExpander) {
        return new SearchService(indexingService, queryExpander);
    }

    @Bean
    public OllamaClient ollamaClient(@Value("${ollama.url}") String url, @Value("${ollama.model}") String model) {
        return new OllamaClient(url, model);
    }

    @Bean
    public QueryExpander queryExpander(OllamaClient ollamaClient) {
        return new QueryExpander(ollamaClient);
    }
}