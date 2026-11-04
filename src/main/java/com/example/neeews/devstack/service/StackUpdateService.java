package com.example.neeews.devstack.service;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.devstack.domain.StackUpdateType;
import com.example.neeews.devstack.dto.response.StackUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StackUpdateService {

    // 추적 저장소는 한 달에 정식 릴리스를 1~3번 낸다. 이보다 오래된 건 소식이 아니다.
    private static final int RELEASE_WINDOW_DAYS = 30;
    private static final int ADVISORY_WINDOW_DAYS = 90;
    private static final String MAVEN = "maven";

    private final GitHubClient gitHubClient;

    @Value("#{'${app.devstack.repositories}'.split(',')}")
    private List<String> repositories;

    @Value("#{'${app.devstack.maven-packages}'.split(',')}")
    private List<String> mavenPackages;

    // 출처마다 마지막으로 성공한 결과를 따로 둔다. 한도에 걸린 출처만 직전 결과를 유지해야
    // 저장소 하나가 실패했다고 목록 전체가 비지 않는다.
    private final Map<String, List<StackUpdateResponse>> latestBySource = new ConcurrentHashMap<>();

    public void refresh() {
        LocalDateTime now = PublishedTime.now();
        for (String repository : repositories) {
            List<GitHubClient.Release> releases = gitHubClient.findReleases(repository);
            if (releases == null) continue;
            latestBySource.put(repository, releases.stream()
                    .filter(release -> !release.prerelease() && !release.draft() && release.publishedAt() != null)
                    .map(release -> StackUpdateResponse.ofRelease(repository, release))
                    .filter(update -> update.getPublishedAt().isAfter(now.minusDays(RELEASE_WINDOW_DAYS)))
                    .toList());
        }

        List<GitHubClient.Advisory> advisories = gitHubClient.findAdvisories(MAVEN, mavenPackages);
        if (advisories != null) {
            Set<String> tracked = Set.copyOf(mavenPackages);
            latestBySource.put(MAVEN, advisories.stream()
                    .filter(advisory -> advisory.withdrawnAt() == null && advisory.publishedAt() != null)
                    .map(advisory -> StackUpdateResponse.ofAdvisory(advisory, tracked))
                    .filter(update -> update.getPublishedAt().isAfter(now.minusDays(ADVISORY_WINDOW_DAYS)))
                    .toList());
        }
    }

    public List<StackUpdateResponse> getUpdates(String type) {
        StackUpdateType filter = type == null ? null : parseType(type);
        return newestFirst().filter(update -> filter == null || update.getType() == filter).toList();
    }

    public List<StackUpdateResponse> getUpdatesSince(LocalDateTime since) {
        return newestFirst().filter(update -> update.getPublishedAt().isAfter(since)).toList();
    }

    private Stream<StackUpdateResponse> newestFirst() {
        return latestBySource.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(StackUpdateResponse::getPublishedAt).reversed());
    }

    private static StackUpdateType parseType(String type) {
        try {
            return StackUpdateType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type은 RELEASE 또는 ADVISORY만 지정할 수 있습니다.");
        }
    }
}
