package com.example.neeews.articleread.domain;

import com.example.neeews.article.domain.Article;
import com.example.neeews.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_reads",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"}),
       indexes = @Index(name = "idx_article_read_user_last_read", columnList = "user_id, lastReadAt"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ArticleRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastReadAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastReadAt = now;
    }

    public void touchReadAt() {
        this.lastReadAt = LocalDateTime.now();
    }
}
