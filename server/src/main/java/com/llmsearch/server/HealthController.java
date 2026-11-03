package com.llmsearch.server;

import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class HealthController {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        String ollamaUrl = System.getenv().getOrDefault("OLLAMA_URL", "http://localhost:11434");
        return Map.of("status", "ok", "ollama", isOllamaUp(ollamaUrl));
    }

    private boolean isOllamaUp(String baseURL) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseURL))
                    .timeout(Duration.ofSeconds(2)).GET()
                    .build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

}
