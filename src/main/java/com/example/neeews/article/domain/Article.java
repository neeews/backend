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
}
