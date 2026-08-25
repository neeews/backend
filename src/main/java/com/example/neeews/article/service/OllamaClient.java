package com.example.neeews.article.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(180);
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_OUTPUT_TOKENS = 200;
    private static final String KEEP_ALIVE = "5m";

    private final RestClient restClient;
    private final String model;

    public OllamaClient(@Value("${app.summary.ollama-url}") String ollamaUrl,
                        @Value("${app.summary.model}") String model) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(ollamaUrl)
                .requestFactory(factory)
                .build();
        this.model = model;
    }

    public String generate(String prompt) {
        try {
            OllamaResponse body = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "prompt", prompt,
                            "stream", false,
                            "keep_alive", KEEP_ALIVE,
                            "options", Map.of(
                                    "temperature", TEMPERATURE,
                                    "num_predict", MAX_OUTPUT_TOKENS)))
                    .retrieve()
                    .body(OllamaResponse.class);

            if (body == null || body.response() == null || body.response().isBlank()) return null;
            return body.response().trim();
        } catch (Exception e) {
            log.warn("[요약] Ollama 호출 실패 model={}: {}", model, e.getMessage());
            return null;
        }
    }

    private record OllamaResponse(String response) {}
}
