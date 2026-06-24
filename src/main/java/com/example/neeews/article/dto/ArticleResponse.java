package com.example.neeews.article.dto;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArticleResponse {

    private Long id;
    private String title;
    private String summary;
    private String category;
    private String imageUrl;
    private String source;
    private LocalDateTime publishedAt;

    public static ArticleResponse from(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(extractSummary(article.getDescription()))
                .category(article.getCategory())
                .imageUrl(article.getImageUrl())
                .source(article.getSource().getDisplayName())
                .publishedAt(article.getPublishedAt())
                .build();
    }

    private static String extractSummary(String description) {
        if (description == null) return null;
        String plain = description.replaceAll("<[^>]*>", "").trim();
        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }
}
