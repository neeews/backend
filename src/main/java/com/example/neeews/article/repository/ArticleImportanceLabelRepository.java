package com.example.neeews.article.repository;

import com.example.neeews.article.domain.ArticleImportanceLabel;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.domain.LabelOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleImportanceLabelRepository extends JpaRepository<ArticleImportanceLabel, Long> {

    List<ArticleImportanceLabel> findByLabel(Importance label);

    long countByLabel(Importance label);

    Optional<ArticleImportanceLabel> findByArticle_IdAndLabeledByAndRound(Long articleId, String labeledBy, int round);

    boolean existsByArticle_IdAndOrigin(Long articleId, LabelOrigin origin);

    List<ArticleImportanceLabel> findByLabeledByAndRound(String labeledBy, int round);

    List<ArticleImportanceLabel> findByOriginAndArticle_IdIn(LabelOrigin origin, Collection<Long> articleIds);

    long countByLabeledByAndRound(String labeledBy, int round);

    long countByOrigin(LabelOrigin origin);

    // 라벨 쏠림 확인용 — 등급별 개수
    @Query("SELECT l.label, COUNT(l) FROM ArticleImportanceLabel l GROUP BY l.label")
    List<Object[]> findLabelDistribution();

    @Query("SELECT l.label, COUNT(l) FROM ArticleImportanceLabel l " +
           "WHERE l.labeledBy = :labeledBy AND l.round = :round GROUP BY l.label")
    List<Object[]> findLabelDistributionBy(@Param("labeledBy") String labeledBy, @Param("round") int round);
}
