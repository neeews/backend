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

    // 카테고리는 RSS 피드마다 박힌 고정값으로 먼저 채워지고, 분류 배치가 본문을 보고 다시 정한다.
    // 이 값이 차 있으면 category가 모델이 정한 값이라는 뜻이다. 피드 원본 값은 source에서 언제든 되찾을 수 있다.
    private LocalDateTime aiCategoryAt;

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
        this.description = description;
        this.contentCrawled = true;
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

    public void updateAiCategory(String category) {
        this.category = category;
        this.aiCategoryAt = LocalDateTime.now();
    }

    public void updateAiImportance(Importance aiImportance, Double aiImportanceScore) {
        this.aiImportance = aiImportance;
        this.aiImportanceScore = aiImportanceScore;
        this.aiImportanceAt = LocalDateTime.now();
    }
}
