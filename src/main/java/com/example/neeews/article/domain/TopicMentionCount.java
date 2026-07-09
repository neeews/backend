package com.example.neeews.article.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "article_topic_mention_counts",
       uniqueConstraints = @UniqueConstraint(name = "uk_topic_date_word", columnNames = {"date", "word"}),
       indexes = @Index(name = "idx_topic_mention_date", columnList = "date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TopicMentionCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    @Builder.Default
    private long count = 0;

    public void addCount(long delta) {
        this.count += delta;
    }
}
