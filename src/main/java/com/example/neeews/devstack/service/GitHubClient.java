package com.example.neeews.devstack.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class GitHubClient {

    private static final String BASE_URL = "https://api.github.com";
    // GitHub API는 User-Agent가 없는 요청을 거절한다.
    private static final String USER_AGENT = "neeews-bot";
    private static final String API_VERSION = "2022-11-28";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final int RELEASES_PER_REPOSITORY = 10;
    private static final int ADVISORIES_LIMIT = 30;

    private static final ParameterizedTypeReference<List<Release>> RELEASE_LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Advisory>> ADVISORY_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    // 토큰 없이도 되지만 한도가 시간당 60회다. 추적 저장소를 크게 늘리면 GITHUB_TOKEN을 넣어 5,000회로 올린다.
    public GitHubClient(@Value("${app.devstack.github-token}") String token) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build());
        factory.setReadTimeout(READ_TIMEOUT);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", API_VERSION)
                .requestFactory(factory);
        if (!token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        this.restClient = builder.build();
    }

    // repository는 "spring-projects/spring-boot" 형식이다.
    public List<Release> findReleases(String repository) {
        return fetchList(uri -> uri.path("/repos/" + repository + "/releases")
                .queryParam("per_page", RELEASES_PER_REPOSITORY)
                .build(), RELEASE_LIST, repository);
    }

    // affects에 패키지를 쉼표로 이어 한 번에 묻는다. 패키지마다 따로 부르면 한도를 패키지 수만큼 쓴다.
    public List<Advisory> findAdvisories(String ecosystem, List<String> packages) {
        return fetchList(uri -> uri.path("/advisories")
                .queryParam("ecosystem", ecosystem)
                .queryParam("affects", String.join(",", packages))
                .queryParam("sort", "published")
                .queryParam("direction", "desc")
                .queryParam("per_page", ADVISORIES_LIMIT)
                .build(), ADVISORY_LIST, "advisories " + ecosystem);
    }

    // 실패하면 null이다. 빈 목록(새 소식 없음)과 구분해야 호출부가 직전 결과를 지우지 않는다.
    private <T> List<T> fetchList(Function<UriBuilder, URI> uri, ParameterizedTypeReference<List<T>> type, String label) {
        try {
            List<T> body = restClient.get().uri(uri).retrieve().body(type);
            return body == null ? List.of() : body;
        } catch (Exception e) {
            log.warn("[GitHub] {} 조회 실패: {}", label, e.getMessage());
            return null;
        }
    }

    public record Release(@JsonProperty("tag_name") String tagName, boolean prerelease, boolean draft,
                          @JsonProperty("published_at") Instant publishedAt,
                          @JsonProperty("html_url") String htmlUrl) {}

    public record Advisory(@JsonProperty("ghsa_id") String ghsaId, @JsonProperty("cve_id") String cveId,
                           @JsonProperty("html_url") String htmlUrl, String summary, String severity,
                           @JsonProperty("published_at") Instant publishedAt,
                           @JsonProperty("withdrawn_at") Instant withdrawnAt,
                           List<Vulnerability> vulnerabilities) {}

    public record Vulnerability(@JsonProperty("package") PackageRef pkg,
                                @JsonProperty("vulnerable_version_range") String vulnerableVersionRange,
                                @JsonProperty("first_patched_version") String firstPatchedVersion) {}

    public record PackageRef(String ecosystem, String name) {}
}
