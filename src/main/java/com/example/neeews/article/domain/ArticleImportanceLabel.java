package com.example.neeews.article.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_importance_labels",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_importance_label_article_labeler_round",
                columnNames = {"article_id", "labeled_by", "round_no"}),
        indexes = {
                @Index(name = "idx_importance_label_label", columnList = "label"),
                @Index(name = "idx_importance_label_origin", columnList = "origin"),
                @Index(name = "idx_importance_label_article", columnList = "article_id")
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

    // 누가 매겼는지. AI 라벨과 사람 라벨이 한 테이블에 쌓이므로 이게 없으면 정답지를 골라낼 수 없다.
    @Column(name = "labeled_by", nullable = false, length = 100)
    private String labeledBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LabelOrigin origin;

    // 같은 사람이 같은 기사를 다시 매긴 회차. 회차 간 일치율이 라벨 품질의 상한선이 된다.
    // round 는 일부 DB에서 예약어라 컬럼명을 round_no 로 둔다.
    @Column(name = "round_no", nullable = false)
    @Builder.Default
    private int round = 0;

    // 라벨을 매길 때 적용한 기준 문서 판본. 기준이 바뀌면 이전 라벨은 다른 잣대로 매겨진 것이다.
    @Column(name = "guide_version", length = 40)
    private String guideVersion;

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
