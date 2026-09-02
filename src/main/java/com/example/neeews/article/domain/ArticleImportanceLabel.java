package com.example.neeews.article.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_importance_labels",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_importance_label_article_labeler",
               columnNames = {"article_id", "labeled_by"}),
       indexes = {
               @Index(name = "idx_importance_label_article", columnList = "article_id"),
               @Index(name = "idx_importance_label_source", columnList = "source"),
               @Index(name = "idx_importance_label_labeled_by", columnList = "labeled_by")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ArticleImportanceLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Importance label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportanceSource source;

    // 라벨을 매긴 주체. AI면 모델 버전("kobert-v1"), 사람이면 식별자(이메일).
    // 같은 기사에 같은 주체가 두 번 라벨하지 못하도록 article_id와 묶어 유니크로 잡았다.
    @Column(name = "labeled_by", nullable = false, length = 100)
    private String labeledBy;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateLabel(Importance label, Double confidence, String note) {
        this.label = label;
        this.confidence = confidence;
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }
}
