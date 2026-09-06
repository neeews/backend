package com.example.neeews.article.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MuniClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final RestClient restClient;

    public MuniClient(@Value("${app.importance.muni-url}") String muniUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(muniUrl)
                .requestFactory(factory)
                .build();
    }

    // 기사 텍스트마다 '중요' 확률(0~1)을 돌려준다. 호출이 실패하면 null — 호출부가 다음 배치로 미룬다.
    public List<Double> score(List<String> texts) {
        try {
            MuniResponse body = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", texts))
                    .retrieve()
                    .body(MuniResponse.class);

            if (body == null || body.results() == null) {
                log.warn("[주요기사] muni 응답이 비어 있음");
                return null;
            }
            if (body.results().size() != texts.size()) {
                log.warn("[주요기사] muni 응답 건수 불일치 요청={} 응답={}",
                        texts.size(), body.results().size());
                return null;
            }
            return body.results().stream().map(MuniResult::score).toList();
        } catch (Exception e) {
            log.warn("[주요기사] muni 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private record MuniResponse(List<MuniResult> results) {}

    private record MuniResult(Double score) {}
}
