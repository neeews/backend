package com.example.neeews.keyword.repository;

import com.example.neeews.keyword.domain.DailyKeywordCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyKeywordCountRepository extends JpaRepository<DailyKeywordCount, Long> {

    List<DailyKeywordCount> findByDate(LocalDate date);
}
