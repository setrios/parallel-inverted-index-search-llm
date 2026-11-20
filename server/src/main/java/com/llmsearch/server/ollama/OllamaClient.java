package com.llmsearch.server.ollama;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class OllamaClient {
    public record GenerateRequest(String model, String prompt, boolean stream, Map<String, Object> options) {}

    @JsonIgnoreProperties(ignoreUnknown = true) // skip many other ollama fields we don't need
    public record GenerateResponse(String response) {}

    private final RestClient http;
    private final String model;
    // true for fair lock so arriving requests will be fulfilled in queue order
    private final ReentrantLock requestLock = new ReentrantLock(true);

    public OllamaClient(String baseUrl, String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.model = model;
    }

    public boolean isUp() {
        try {
            http.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    public String generate(String prompt) {
        requestLock.lock();
        try {
            GenerateResponse r = http.post()
                .uri("/api/generate")
                .body(new GenerateRequest(model, prompt, false, Map.of("temperature", 0, "num_predict", 40)))
                .retrieve()
                .body(GenerateResponse.class);
            return r == null ? "" : r.response();
        } finally {
            requestLock.unlock();
        }
    }
}
