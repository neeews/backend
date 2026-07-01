package com.example.neeews.article.repository;

import com.example.neeews.article.domain.Article;
import com.example.neeews.rss.domain.NewsSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByLink(String link);

    java.util.Optional<Article> findByLink(String link);

    Page<Article> findBySource(NewsSource source, Pageable pageable);

    List<Article> findTop10ByOrderByPublishedAtDesc();

    List<Article> findTop5ByOrderByPublishedAtDesc();

    List<Article> findTop6ByOrderByViewCountDesc();

    List<Article> findTop5ByCategoryAndIdNotOrderByPublishedAtDesc(String category, Long id);

    @Query("SELECT a FROM Article a WHERE (:category IS NULL OR a.category = :category)")
    Page<Article> findByCategoryOptional(@Param("category") String category, Pageable pageable);

    @Query(value = "SELECT a FROM Article a WHERE " +
                  "LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                  "LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%')) " +
                  "ORDER BY CASE WHEN LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) THEN 0 ELSE 1 END, a.publishedAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE " +
                        "LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                        "LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Article> searchByKeyword(@Param("q") String q, Pageable pageable);

    @Query(value = "SELECT a FROM Article a WHERE " +
                  "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                  "AND a.source IN :sources " +
                  "ORDER BY CASE WHEN LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) THEN 0 ELSE 1 END, a.publishedAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE " +
                        "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "AND a.source IN :sources")
    Page<Article> searchByKeywordAndSources(@Param("q") String q, @Param("sources") List<NewsSource> sources, Pageable pageable);

    @Query(value = "SELECT a FROM Article a WHERE " +
                  "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                  "AND a.category IN :categories " +
                  "ORDER BY CASE WHEN LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) THEN 0 ELSE 1 END, a.publishedAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE " +
                        "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "AND a.category IN :categories")
    Page<Article> searchByKeywordAndCategories(@Param("q") String q, @Param("categories") List<String> categories, Pageable pageable);

    @Query(value = "SELECT a FROM Article a WHERE " +
                  "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                  "AND a.source IN :sources AND a.category IN :categories " +
                  "ORDER BY CASE WHEN LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) THEN 0 ELSE 1 END, a.publishedAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE " +
                        "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                        "AND a.source IN :sources AND a.category IN :categories")
    Page<Article> searchByKeywordAndSourcesAndCategories(@Param("q") String q, @Param("sources") List<NewsSource> sources, @Param("categories") List<String> categories, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.cachedImagePath IS NOT NULL AND a.lastViewedAt < :threshold")
    List<Article> findExpiredCachedImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT a.category, COUNT(a) FROM Article a GROUP BY a.category ORDER BY COUNT(a) DESC")
    List<Object[]> findCategoryStats();
}
