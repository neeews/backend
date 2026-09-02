package com.example.neeews.article.domain;

import com.example.neeews.rss.domain.NewsSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_article_link", columnList = "link", unique = true),
        @Index(name = "idx_article_source", columnList = "source"),
        @Index(name = "idx_article_published_at", columnList = "publishedAt"),
        @Index(name = "idx_article_category", columnList = "category"),
        @Index(name = "idx_article_importance", columnList = "importance"),
        @Index(name = "idx_article_importance_published", columnList = "importance, publishedAt")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Builder.Default
    @Column(nullable = false)
    private boolean contentCrawled = false;

    // 서비스에 노출할 현재 중요도 한 건. 라벨 이력 전체는 ArticleImportanceLabel에 쌓인다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Importance importance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ImportanceSource importanceSource;

    private LocalDateTime importanceScoredAt;

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

    public void updateImportance(Importance importance, ImportanceSource importanceSource) {
        this.importance = importance;
        this.importanceSource = importanceSource;
        this.importanceScoredAt = LocalDateTime.now();
    }
}
