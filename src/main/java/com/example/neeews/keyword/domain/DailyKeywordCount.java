package com.example.neeews.keyword.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_keyword_counts",
       uniqueConstraints = @UniqueConstraint(name = "uk_keyword_date_word", columnNames = {"date", "word"}),
       indexes = @Index(name = "idx_daily_keyword_date", columnList = "date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyKeywordCount {

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
