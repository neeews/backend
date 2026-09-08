package com.example.neeews.article.domain;

import com.example.neeews.rss.domain.NewsSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_article_link", columnList = "link", unique = true),
        @Index(name = "idx_article_source", columnList = "source"),
        @Index(name = "idx_article_published_at", columnList = "publishedAt"),
        @Index(name = "idx_article_category", columnList = "category")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Article {

    private static final int MAX_DESCRIPTION_LENGTH = 15_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 1000)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String author;

    private String category;

    @Column(length = 1000)
    private String imageUrl;

    // JdbcTypeCode 없이 두면 MySQL enum 컬럼으로 매핑된다. 그러면 NewsSource에 상수를 추가해도
    // ddl-auto=update가 기존 enum 목록을 갱신하지 못해 새 소스의 기사가 전부 저장 실패한다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private NewsSource source;

    private LocalDateTime publishedAt;

    @Builder.Default
    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fetchedAt;

    @Column(length = 500)
    private String cachedImagePath;

    private LocalDateTime lastViewedAt;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private LocalDateTime aiSummarizedAt;

    // 서비스 노출용 중요도. 라벨링 데이터셋(article_importance_labels)과 분리해 둔다.
    // 같은 테이블에 쌓으면 모델 학습·채점용 라벨과 섞여 채점이 오염된다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ai_importance", length = 10)
    private Importance aiImportance;

    // muni가 매긴 '중요' 확률(0~1). 등급은 노출 여부만 가르므로, 같은 등급 안의 순위는 이 값으로 매긴다.
    // 규칙 필터로 확정된 기사는 모델을 안 거쳐 null 이다.
    private Double aiImportanceScore;

    private LocalDateTime aiImportanceAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean contentCrawled = false;

    @PrePersist
    void onCreate() {
        this.fetchedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateCachedImage(String imageUrl, String cachedImagePath) {
        this.imageUrl = imageUrl;
        this.cachedImagePath = cachedImagePath;
    }

    public void clearCachedImage() {
        this.imageUrl = null;
        this.cachedImagePath = null;
    }

    public void updateLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }

    public void updateDescription(String description) {
        this.description = truncateToColumnLimit(description);
        this.contentCrawled = true;
    }

    // description은 TEXT(65,535바이트)라 이를 넘기면 UPDATE 자체가 거부돼 배치가 통째로 죽는다.
    // 한글은 3바이트, 이모지는 4바이트라 문자 수로 자를 땐 최악(4바이트)을 기준으로 잡는다.
    // 본문이 이 길이를 넘는 기사는 크롤러가 페이지 전체를 긁어온 경우라 잘라도 본문이 잘리지 않는다.
    private static String truncateToColumnLimit(String description) {
        if (description == null || description.length() <= MAX_DESCRIPTION_LENGTH) return description;
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    public void markContentCrawled() {
        this.contentCrawled = true;
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
        this.aiSummarizedAt = LocalDateTime.now();
    }

    public void updateCategory(String category) {
        this.category = category;
    }

    public void updateAiImportance(Importance aiImportance, Double aiImportanceScore) {
        this.aiImportance = aiImportance;
        this.aiImportanceScore = aiImportanceScore;
        this.aiImportanceAt = LocalDateTime.now();
    }
}
