package com.example.neeews.article.dto;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ArticleDetailResponse {

    private ArticleDetail article;
    private List<ArticleResponse> related;

    @Getter
    @Builder
    public static class ArticleDetail {
        private Long id;
        private String title;
        private String summary;
        private String content;
        private String category;
        private String imageUrl;
        private String source;
        private String articleUrl;
        private LocalDateTime publishedAt;
        private boolean isBookmarked;
    }

    public static ArticleDetailResponse of(Article article, List<ArticleResponse> related, boolean isBookmarked) {
        String description = article.getDescription();
        String summary = null;
        if (description != null) {
            String plain = HtmlUtils.htmlUnescape(description.replaceAll("<[^>]*>", "")).trim();
            summary = plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
        }
        return ArticleDetailResponse.builder()
                .article(ArticleDetail.builder()
                        .id(article.getId())
                        .title(article.getTitle())
                        .summary(summary)
                        .content(description)
                        .category(article.getCategory())
                        .imageUrl(article.getImageUrl())
                        .source(article.getSource().getDisplayName())
                        .articleUrl(article.getLink())
                        .publishedAt(article.getPublishedAt())
                        .isBookmarked(isBookmarked)
                        .build())
                .related(related)
                .build();
    }
}
