package com.example.neeews.article.dto.response;

import com.example.neeews.article.domain.Article;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LabelingArticleResponse {

    // 라벨링 화면에는 기존 라벨도 모델 예측도 절대 싣지 않는다.
    // 숫자를 먼저 보면 판단이 거기 끌려가고, 그렇게 만든 라벨은 독립적인 정답지가 아니다.
    private Long id;
    private String title;
    private String content;
    private String category;
    private String source;
    private String articleUrl;
    private LocalDateTime publishedAt;

    public static LabelingArticleResponse from(Article article) {
        return LabelingArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getDescription())
                .category(article.getCategory())
                .source(article.getSource() != null ? article.getSource().name() : null)
                .articleUrl(article.getLink())
                .publishedAt(article.getPublishedAt())
                .build();
    }
}
