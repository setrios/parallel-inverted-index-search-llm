package com.llmsearch.server.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.llmsearch.server.search.ExpandedQuery;
import com.llmsearch.server.search.SearchResponse;
import com.llmsearch.server.search.SearchResult;
import com.llmsearch.server.search.SearchService;

import tools.jackson.databind.json.JsonMapper;

public class SearchWebSocketHandler extends TextWebSocketHandler {
    public record SearchRequest(String query) {}

    @JsonInclude(JsonInclude.Include.NON_NULL) // "done" has no data, so no "data" key at all
    public record StageMessage(String stage, Object data) {}

    private final SearchService search;
    private final JsonMapper json = JsonMapper.builder().build(); // as spring doens't autoconvert jsons for websockets

    public SearchWebSocketHandler(SearchService search) {
        this.search = search;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            SearchRequest request = json.readValue(message.getPayload(), SearchRequest.class);

            ExpandedQuery q = search.expandQuery(request.query());
            send(session, new StageMessage("query_expansion", q));

            long start = System.nanoTime();
            List<SearchResult> results = search.keywordSearch(q);
            long ms = (System.nanoTime() - start) / 1000000;
            send(session, new StageMessage("keyword_search", new SearchResponse(results, ms)));
        } catch (Exception e) {
            send(session, new StageMessage("error", Map.of("message", String.valueOf(e.getMessage()))));
        }
        send(session, new StageMessage("done", null));
    }

    private void send(WebSocketSession session, StageMessage message) throws IOException {
        session.sendMessage(new TextMessage(json.writeValueAsString(message)));
    }
}