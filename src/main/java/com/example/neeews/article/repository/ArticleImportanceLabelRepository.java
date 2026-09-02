package com.example.neeews.article.repository;

import com.example.neeews.article.domain.ArticleImportanceLabel;
import com.example.neeews.article.domain.ImportanceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleImportanceLabelRepository extends JpaRepository<ArticleImportanceLabel, Long> {

    List<ArticleImportanceLabel> findByArticleId(Long articleId);

    Optional<ArticleImportanceLabel> findByArticleIdAndLabeledBy(Long articleId, String labeledBy);

    boolean existsByArticleIdAndLabeledBy(Long articleId, String labeledBy);

    List<ArticleImportanceLabel> findBySource(ImportanceSource source);

    long countBySource(ImportanceSource source);

    long countByLabeledBy(String labeledBy);

    // 라벨 쏠림 확인용 — 특정 주체가 매긴 라벨의 등급별 개수
    @Query("SELECT l.label, COUNT(l) FROM ArticleImportanceLabel l " +
           "WHERE l.labeledBy = :labeledBy GROUP BY l.label")
    List<Object[]> findLabelDistributionByLabeledBy(@Param("labeledBy") String labeledBy);
}
