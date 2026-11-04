package com.example.neeews.devstack.dto.response;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.devstack.domain.StackUpdateType;
import com.example.neeews.devstack.service.GitHubClient;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 릴리스와 보안 권고를 한 목록에 섞어 시간순으로 보여주려고 한 형태로 내린다.
// severity·identifier·affected는 보안 권고에만 있다.
@Getter
@Builder
public class StackUpdateResponse {

    private StackUpdateType type;
    private String project;
    private String title;
    private String severity;
    private String identifier;
    private String affected;
    private String url;
    private LocalDateTime publishedAt;

    public static StackUpdateResponse ofRelease(String repository, GitHubClient.Release release) {
        return StackUpdateResponse.builder()
                .type(StackUpdateType.RELEASE)
                .project(repository.substring(repository.indexOf('/') + 1))
                .title(release.tagName())
                .url(release.htmlUrl())
                .publishedAt(LocalDateTime.ofInstant(release.publishedAt(), PublishedTime.ZONE))
                .build();
    }

    // 권고 하나가 여러 패키지에 걸쳐 있다(jackson 2.x·3.x, webmvc·webflux 등). 추적하는 패키지만 남겨야
    // 내 스택과 상관없는 범위까지 읽지 않는다.
    public static StackUpdateResponse ofAdvisory(GitHubClient.Advisory advisory, Set<String> trackedPackages) {
        List<GitHubClient.Vulnerability> relevant = advisory.vulnerabilities() == null ? List.of()
                : advisory.vulnerabilities().stream()
                        .filter(v -> v.pkg() != null && trackedPackages.contains(v.pkg().name()))
                        .toList();
        return StackUpdateResponse.builder()
                .type(StackUpdateType.ADVISORY)
                .project(relevant.stream().map(v -> artifactOf(v.pkg().name())).distinct().collect(Collectors.joining(", ")))
                .title(advisory.summary())
                .severity(advisory.severity())
                .identifier(advisory.cveId() != null ? advisory.cveId() : advisory.ghsaId())
                .affected(relevant.stream().map(StackUpdateResponse::describe).collect(Collectors.joining(" / ")))
                .url(advisory.htmlUrl())
                .publishedAt(LocalDateTime.ofInstant(advisory.publishedAt(), PublishedTime.ZONE))
                .build();
    }

    // "jackson-databind >= 3.0.0, <= 3.1.6 → 3.1.7에서 수정"
    private static String describe(GitHubClient.Vulnerability vulnerability) {
        String patched = vulnerability.firstPatchedVersion() == null
                ? "수정 버전 없음"
                : vulnerability.firstPatchedVersion() + "에서 수정";
        return artifactOf(vulnerability.pkg().name()) + " " + vulnerability.vulnerableVersionRange() + " → " + patched;
    }

    private static String artifactOf(String coordinate) {
        return coordinate.substring(coordinate.lastIndexOf(':') + 1);
    }
}
