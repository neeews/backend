package com.example.neeews.visit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_visits",
       uniqueConstraints = @UniqueConstraint(name = "uk_daily_visit_date", columnNames = "visit_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DailyVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visitor_count", nullable = false)
    @Builder.Default
    private long visitorCount = 0;
}
