package com.example.neeews.article.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_importance_labels", indexes = {
        @Index(name = "idx_importance_label_label", columnList = "label")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ArticleImportanceLabel {

    // 기사 1건당 라벨 1건이므로 article_id를 그대로 PK로 쓴다.
    // @MapsId로 PK와 FK를 같은 컬럼에 묶어, 별도 대리키나 유니크 제약 없이 중복 라벨을 막는다.
    @Id
    @Column(name = "article_id")
    private Long articleId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Importance label;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateLabel(Importance label) {
        this.label = label;
        this.updatedAt = LocalDateTime.now();
    }
}
