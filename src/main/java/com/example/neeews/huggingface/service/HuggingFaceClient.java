package com.example.neeews.huggingface.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class HuggingFaceClient {

    private static final String BASE_URL = "https://huggingface.co";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; NeeewsBot/1.0)";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    // nvidia는 한 달에 40개 넘게 올린다. 이만큼은 받아야 좋아요가 쌓이는 동안 최신 목록에서 밀려나지 않는다.
    private static final int ORG_MODELS_LIMIT = 50;
    // 양자화 사본을 걸러내도 10개가 남도록 넉넉히 받는다. 상위 30개 중 양자화가 7개였다.
    private static final int TRENDING_FETCH_LIMIT = 30;
    // expand를 붙이면 지정한 필드만 내려온다. 파라미터 수(safetensors)·라이선스 이름(cardData)은 기본 응답에 없어 쓰는 필드를 모두 적는다.
    // siblings(파일 목록)도 받을 수 있지만 응답이 여섯 배 가까이 커져(Qwen 50개 기준 22KB → 132KB) 넣지 않는다.
    private static final Object[] MODEL_FIELDS = {"likes", "downloads", "createdAt", "tags", "pipeline_tag", "safetensors", "cardData"};
    private static final String QUANTIZED_TAG_PREFIX = "base_model:quantized:";
    private static final String BASE_MODEL_TAG_PREFIX = "base_model:";
    private static final String LICENSE_TAG_PREFIX = "license:";
    private static final String MODEL_CARD_SELECTOR = ".model-card-content";

    private static final ParameterizedTypeReference<List<ModelInfo>> MODEL_LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<DailyPaper>> PAPER_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public HuggingFaceClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build());
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .requestFactory(factory)
                .build();
    }

    public List<ModelInfo> findLatestModels(String author) {
        return fetchList(uri -> uri.path("/api/models")
                .queryParam("author", author)
                .queryParam("sort", "createdAt")
                .queryParam("direction", -1)
                .queryParam("limit", ORG_MODELS_LIMIT)
                .queryParam("expand[]", MODEL_FIELDS)
                .build(), MODEL_LIST, author);
    }

    public List<ModelInfo> findTrendingModels() {
        return fetchList(uri -> uri.path("/api/models")
                .queryParam("sort", "trendingScore")
                .queryParam("direction", -1)
                .queryParam("limit", TRENDING_FETCH_LIMIT)
                .queryParam("expand[]", MODEL_FIELDS)
                .build(), MODEL_LIST, "trending");
    }

    // period가 null이면 기간 없이 최신순 전체, 있으면 그 기간(date·week·month)의 추천순이다.
    public List<DailyPaper> findPapers(String period, String value, int pageIndex, int size) {
        return fetchList(uri -> {
            uri.path("/api/daily_papers")
                    .queryParam("limit", size)
                    .queryParam("p", pageIndex);
            if (period != null) uri.queryParam(period, value);
            return uri.build();
        }, PAPER_LIST, "papers " + (period == null ? "latest" : period + "=" + value) + " p=" + pageIndex);
    }

    // 동의가 필요한(gated) 모델은 README 원문 요청을 401로 막지만, 모델 페이지에는 카드가 그대로 렌더링돼 있다.
    // Llama·Gemma가 전부 gated라 페이지에서 읽는다. 못 받으면 null — 호출부가 다음 주기에 다시 시도한다.
    public String findModelCardText(String modelId) {
        String html;
        try {
            html = restClient.get()
                    .uri(uri -> uri.path("/" + modelId).build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("[HuggingFace] {} 모델 페이지 조회 실패: {}", modelId, e.getMessage());
            return null;
        }
        if (html == null) return null;

        Element card = Jsoup.parse(html).selectFirst(MODEL_CARD_SELECTOR);
        if (card == null) {
            // HF가 마크업을 바꾸면 모든 모델의 요약이 멈추므로, 셀렉터가 낡았음을 남긴다.
            log.warn("[HuggingFace] 모델 카드 영역 미발견 selector={} model={}", MODEL_CARD_SELECTOR, modelId);
            return null;
        }
        // 코드 예제·벤치마크 표·이미지는 소개가 아니다. 인라인 코드는 모델 이름이 들어 있어 남긴다.
        card.select("pre, table, img, svg").remove();
        return card.text();
    }

    // HF가 응답하지 않으면 빈 목록을 돌려준다. 호출부는 직전 데이터를 그대로 두고 다음 주기에 다시 받는다.
    private <T> List<T> fetchList(Function<UriBuilder, URI> uri, ParameterizedTypeReference<List<T>> type, String label) {
        try {
            List<T> body = restClient.get().uri(uri).retrieve().body(type);
            return body == null ? List.of() : body;
        } catch (Exception e) {
            log.warn("[HuggingFace] {} 조회 실패: {}", label, e.getMessage());
            return List.of();
        }
    }

    // cardData는 모델 카드 머리말(YAML)을 옮긴 것이라 저장소마다 값의 형태가 다르다(base_model은 문자열이기도, 배열이기도 하다).
    // 고정 타입으로 받으면 모델 하나 때문에 조직 목록 전체가 파싱에 실패하므로 JsonNode로 받아 문자열일 때만 쓴다.
    public record ModelInfo(String id, long likes, long downloads, List<String> tags,
                            @JsonProperty("pipeline_tag") String pipelineTag, Instant createdAt,
                            Safetensors safetensors, JsonNode cardData) {

        // FP8·GGUF 등으로 압축한 사본이다. 원본과 같은 출시라 따로 다루지 않는다.
        public boolean isQuantized() {
            return tags != null && tags.stream().anyMatch(tag -> tag.startsWith(QUANTIZED_TAG_PREFIX));
        }

        // "gpt2"처럼 조직 없이 올라간 초창기 모델은 id에 '/'가 없다.
        public String author() {
            int slash = id.indexOf('/');
            return slash < 0 ? null : id.substring(0, slash);
        }

        public Long parameterCount() {
            return safetensors == null ? null : safetensors.total();
        }

        // 파생 관계 태그("base_model:finetune:Qwen/Qwen3.5-4B")와 함께 원본 id만 담은 태그("base_model:Qwen/Qwen3.5-4B")가 붙는다.
        // 모델 id에는 ':'가 없으므로 접두사 뒤에 ':'가 없는 태그가 원본이다.
        public String baseModel() {
            return tagValues(BASE_MODEL_TAG_PREFIX).stream()
                    .filter(value -> !value.contains(":"))
                    .findFirst()
                    .orElse(null);
        }

        public String licenseId() {
            return tagValues(LICENSE_TAG_PREFIX).stream().findFirst().orElse(null);
        }

        // 직접 만든 라이선스는 id가 "other"뿐이라 이름("qwen-research")을 따로 봐야 무엇인지 안다.
        public String licenseName() {
            return cardText("license_name");
        }

        // license_link는 저장소 안의 파일 경로("LICENSE")이거나 외부 주소다. 없으면 라이선스가 표시되는 모델 페이지로 보낸다.
        public String licenseUrl() {
            String modelUrl = BASE_URL + "/" + id;
            String link = cardText("license_link");
            if (link == null) return modelUrl;
            if (link.startsWith("http://") || link.startsWith("https://")) return link;
            return modelUrl + "/blob/main/" + link.replaceFirst("^\\.?/", "");
        }

        private List<String> tagValues(String prefix) {
            if (tags == null) return List.of();
            return tags.stream()
                    .filter(tag -> tag.startsWith(prefix))
                    .map(tag -> tag.substring(prefix.length()))
                    .toList();
        }

        private String cardText(String field) {
            if (cardData == null) return null;
            JsonNode node = cardData.path(field);
            return node.isString() && !node.asString().isBlank() ? node.asString().trim() : null;
        }
    }

    public record Safetensors(Long total) {}

    public record DailyPaper(String title, String thumbnail, Paper paper) {}

    public record Paper(String id, int upvotes, String summary,
                        @JsonProperty("ai_summary") String aiSummary, Instant publishedAt,
                        Instant submittedOnDailyAt) {}
}
