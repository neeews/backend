package com.example.neeews.article.repository;

import com.example.neeews.article.domain.ArticleImportanceLabel;
import com.example.neeews.article.domain.Importance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleImportanceLabelRepository extends JpaRepository<ArticleImportanceLabel, Long> {

    List<ArticleImportanceLabel> findByLabel(Importance label);

    long countByLabel(Importance label);

    // 라벨 쏠림 확인용 — 등급별 개수
    @Query("SELECT l.label, COUNT(l) FROM ArticleImportanceLabel l GROUP BY l.label")
    List<Object[]> findLabelDistribution();
}
