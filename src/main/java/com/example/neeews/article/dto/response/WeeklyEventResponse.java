package com.example.neeews.article.dto.response;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// 오늘의 뉴스처럼 요약문을 이어 읽는 화면이라 카드용 필드(제목·이미지)는 내리지 않는다.
// 같은 사건을 다룬 기사 id를 함께 내려, 그중 하나라도 읽었으면 읽은 사건으로 표시한다.
@Getter
@Builder(toBuilder = true)
public class WeeklyEventResponse {

    private Long id;
    private String summary;
    private long sourceCount;
    private List<Long> articleIds;
    private LocalDateTime firstPublishedAt;
    private LocalDateTime lastPublishedAt;
    private boolean isRead;

    // 묶음은 중요도 점수 높은 순이라 첫 기사가 사건의 대표다.
    public static WeeklyEventResponse of(List<Article> cluster, long sourceCount) {
        Article representative = cluster.get(0);
        return WeeklyEventResponse.builder()
                .id(representative.getId())
                .summary(representative.getAiSummary())
                .sourceCount(sourceCount)
                .articleIds(cluster.stream().map(Article::getId).toList())
                .firstPublishedAt(cluster.stream().map(Article::getPublishedAt).min(Comparator.naturalOrder()).orElse(null))
                .lastPublishedAt(cluster.stream().map(Article::getPublishedAt).max(Comparator.naturalOrder()).orElse(null))
                .build();
    }

    public WeeklyEventResponse withRead(boolean isRead) {
        return toBuilder().isRead(isRead).build();
    }
}
