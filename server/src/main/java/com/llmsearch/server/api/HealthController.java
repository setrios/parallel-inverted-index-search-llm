package com.llmsearch.server.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.llmsearch.server.ollama.OllamaClient;

@RestController
public class HealthController {
    public record HealthResponse(String status, boolean ollama) {}

    private final OllamaClient ollama;
    
    public HealthController(OllamaClient ollama) {
        this.ollama = ollama;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("ok", ollama.isUp());
    }
}
