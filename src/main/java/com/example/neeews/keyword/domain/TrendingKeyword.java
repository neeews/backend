package com.example.neeews.keyword.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "trending_keywords",
       indexes = @Index(name = "idx_keyword_date_rank", columnList = "date, keyword_rank"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TrendingKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword_rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private String word;

    @Column(name = "keyword_change", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KeywordChange change = KeywordChange.NEW;

    @Column(nullable = false)
    private LocalDate date;

    public enum KeywordChange {
        up, down, same, NEW
    }
}
