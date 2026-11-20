package com.llmsearch.server.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.llmsearch.server.search.SearchService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final SearchService search;

    public WebSocketConfig(SearchService search) {
        this.search = search;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SearchWebSocketHandler(search), "/ws/search").setAllowedOrigins("*");
    }
}