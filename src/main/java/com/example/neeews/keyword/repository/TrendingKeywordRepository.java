package com.example.neeews.keyword.repository;

import com.example.neeews.keyword.domain.TrendingKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrendingKeywordRepository extends JpaRepository<TrendingKeyword, Long> {

    List<TrendingKeyword> findByDateOrderByRankAsc(LocalDate date);

    void deleteByDate(LocalDate date);
}
