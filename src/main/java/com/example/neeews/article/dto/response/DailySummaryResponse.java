package com.example.neeews.article.dto.response;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DailySummaryResponse {

    private Long id;
    private String title;
    private String summary;
    private String category;
    private String imageUrl;
    private String source;
    private LocalDateTime publishedAt;
    private LocalDateTime summarizedAt;

    public static DailySummaryResponse from(Article article) {
        return DailySummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getAiSummary())
                .category(article.getCategory())
                .imageUrl(article.getImageUrl())
                .source(article.getSource().getDisplayName())
                .publishedAt(article.getPublishedAt())
                .summarizedAt(article.getAiSummarizedAt())
                .build();
    }
}
