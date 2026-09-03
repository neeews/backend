package com.example.neeews.article.dto.response;

import com.example.neeews.article.domain.ArticleImportanceLabel;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.domain.LabelOrigin;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ImportanceLabelResponse {

    private Long articleId;
    private Importance label;
    private String labeledBy;
    private LabelOrigin origin;
    private int round;
    private String guideVersion;
    private LocalDateTime createdAt;

    public static ImportanceLabelResponse from(ArticleImportanceLabel label) {
        return ImportanceLabelResponse.builder()
                .articleId(label.getArticle().getId())
                .label(label.getLabel())
                .labeledBy(label.getLabeledBy())
                .origin(label.getOrigin())
                .round(label.getRound())
                .guideVersion(label.getGuideVersion())
                .createdAt(label.getCreatedAt())
                .build();
    }
}
