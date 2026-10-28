package com.example.neeews.article.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class HeadlineSectionResponse {

    private String category;
    private String title;
    private List<ArticleResponse> articles;

    public static HeadlineSectionResponse of(String category, List<ArticleResponse> articles) {
        return HeadlineSectionResponse.builder()
                .category(category)
                .title(category + " 주요기사")
                .articles(articles)
                .build();
    }

    public HeadlineSectionResponse withArticles(List<ArticleResponse> articles) {
        return toBuilder().articles(articles).build();
    }
}
