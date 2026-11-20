package com.llmsearch.server.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    public record HealthResponse(String status, boolean ollama) {}

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("ok", false);
    }
}
