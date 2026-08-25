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

    List<Article> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Article> findTop5ByOrderByPublishedAtDesc();

    List<Article> findTop6ByPublishedAtAfterOrderByPublishedAtDesc(LocalDateTime after);

    List<Article> findTop5ByCategoryAndIdNotOrderByPublishedAtDesc(String category, Long id);

    @Query("SELECT a FROM Article a WHERE a.publishedAt >= :since AND " +
                  "(LOWER(a.title) LIKE LOWER(CONCAT('%', :word, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :word, '%'))) " +
                  "ORDER BY a.publishedAt DESC")
    List<Article> findTopByTopicSince(@Param("word") String word, @Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE (:category IS NULL OR a.category = :category)")
    Page<Article> findByCategoryOptional(@Param("category") String category, Pageable pageable);

    // 인기 정렬: HN 방식 점수 (조회수+1) / (경과일수+1)^2.0 — 조회수가 많아도 오래되면 감쇠한다.
    // 최근 :since 이후 기사만 후보로 삼아, 초기 저트래픽 구간에 조회수가 몰린 오래된 기사가 상단을 점유하지 않게 한다.
    // GREATEST(..., 0): RSS 발행시각이 서버 시각(UTC)보다 미래인 기사가 있어 음수 나이를 0으로 클램프
    @Query(value = "SELECT * FROM articles a WHERE (:category IS NULL OR a.category = :category) " +
                  "AND a.published_at >= :since " +
                  "ORDER BY (a.view_count + 1) / POW(GREATEST(TIMESTAMPDIFF(HOUR, a.published_at, NOW()), 0) / 24.0 + 1, 2.0) DESC, a.published_at DESC",
           countQuery = "SELECT COUNT(*) FROM articles a WHERE (:category IS NULL OR a.category = :category) AND a.published_at >= :since",
           nativeQuery = true)
    Page<Article> findByCategoryOrderByPopularity(@Param("category") String category, @Param("since") LocalDateTime since, Pageable pageable);

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
