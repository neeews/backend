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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsSource source;

    private LocalDateTime publishedAt;

    @Builder.Default
    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fetchedAt;

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

    public void updateDescription(String description) {
        this.description = description;
    }
}
