package com.example.neeews.article.dto.response;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;

// 오늘의 뉴스는 기사 카드 목록이 아니라 요약문을 이어 읽는 화면이다. 제목·이미지·출처를 함께 내리면
// 화면이 카드로 그려지므로, 읽을 글(summary)과 눌렀을 때 갈 곳(id)만 남긴다.
@Getter
@Builder
public class DailySummaryResponse {

    private Long id;
    private String summary;

    public static DailySummaryResponse from(Article article) {
        return DailySummaryResponse.builder()
                .id(article.getId())
                .summary(article.getAiSummary())
                .build();
    }
}
