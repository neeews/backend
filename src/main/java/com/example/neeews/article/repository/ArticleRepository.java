package com.example.neeews.article.repository;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.Importance;
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

    List<Article> findTop6ByAiImportanceAndPublishedAtAfterOrderByPublishedAtDesc(
            Importance aiImportance, LocalDateTime after);

    List<Article> findTop6ByAiImportanceIsNullAndPublishedAtAfterOrderByPublishedAtDesc(LocalDateTime after);

    List<Article> findTop5ByCategoryAndIdNotOrderByPublishedAtDesc(String category, Long id);

    // 핫이슈는 급상승 주제 안에서도 muni가 HIGH로 본 기사만 올린다.
    // 주제 키워드는 전체 기사에서 뽑히므로, 중요도를 안 보면 같은 단어를 쓴 지자체·기업 홍보물이 그대로 올라온다.
    @Query("SELECT a FROM Article a WHERE a.publishedAt >= :since " +
                  "AND a.aiImportance = com.example.neeews.article.domain.Importance.HIGH AND " +
                  "(LOWER(a.title) LIKE LOWER(CONCAT('%', :word, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :word, '%'))) " +
                  "ORDER BY a.publishedAt DESC")
    List<Article> findTopByTopicSince(@Param("word") String word, @Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE (:category IS NULL OR a.category = :category)")
    Page<Article> findByCategoryOptional(@Param("category") String category, Pageable pageable);

    // 인기 정렬 1순위는 muni가 매긴 중요도 점수(0~1). 판정 전 기사는 0.5(중립)로 봐서 HIGH 아래, LOW 위에 놓는다.
    // 기존 HN 점수 (조회수+1)/(경과일수+1)^2.0은 로그로 눌러 보조 점수로만 얹고 0.15로 상한을 둔다 —
    // 조회수가 아무리 많아도 중요도 점수 차이(0.15 이상)를 뒤집지 못하게 하려는 것.
    // 최근 :since 이후 기사만 후보로 삼아, 초기 저트래픽 구간에 조회수가 몰린 오래된 기사가 상단을 점유하지 않게 한다.
    // GREATEST(..., 0): RSS 발행시각이 서버 시각(UTC)보다 미래인 기사가 있어 음수 나이를 0으로 클램프
    @Query(value = "SELECT * FROM articles a WHERE (:category IS NULL OR a.category = :category) " +
                  "AND a.published_at >= :since " +
                  "ORDER BY COALESCE(a.ai_importance_score, 0.5) + LEAST(LOG10(" +
                  "(a.view_count + 1) / POW(GREATEST(TIMESTAMPDIFF(HOUR, a.published_at, NOW()), 0) / 24.0 + 1, 2.0) + 1" +
                  ") * 0.05, 0.15) DESC, a.published_at DESC",
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

    Optional<Article> findTop1ByCategoryAndAiSummaryIsNullAndPublishedAtAfterOrderByPublishedAtDesc(
            String category, LocalDateTime after);

    @Query("SELECT a.category, MAX(a.aiSummarizedAt) FROM Article a WHERE a.aiSummary IS NOT NULL GROUP BY a.category")
    List<Object[]> findLastSummarizedAtByCategory();

    List<Article> findTop5ByAiSummaryIsNotNullOrderByAiSummarizedAtDesc();

    // 아직 아무도 라벨을 안 매긴 기사 — AI 자동 라벨링 대상 조회용
    @Query("SELECT a FROM Article a WHERE NOT EXISTS " +
           "(SELECT 1 FROM ArticleImportanceLabel l WHERE l.article = a) " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findUnlabeled(Pageable pageable);

    // 특정 라벨러가 그 회차에 아직 안 매긴 기사. 다른 사람/AI가 매겼는지는 따지지 않는다.
    @Query("SELECT a FROM Article a WHERE NOT EXISTS " +
           "(SELECT 1 FROM ArticleImportanceLabel l WHERE l.article = a " +
           " AND l.labeledBy = :labeledBy AND l.round = :round) " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findUnlabeledBy(@Param("labeledBy") String labeledBy,
                                  @Param("round") int round,
                                  Pageable pageable);

    // 위와 같되 AI가 이미 매긴 기사만 — 시드 라벨이 사람 판단과 얼마나 맞는지 재는 대조군
    @Query("SELECT a FROM Article a WHERE NOT EXISTS " +
           "(SELECT 1 FROM ArticleImportanceLabel l WHERE l.article = a " +
           " AND l.labeledBy = :labeledBy AND l.round = :round) " +
           "AND EXISTS (SELECT 1 FROM ArticleImportanceLabel s WHERE s.article = a " +
           "            AND s.origin = com.example.neeews.article.domain.LabelOrigin.AI) " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findUnlabeledByWithAiLabel(@Param("labeledBy") String labeledBy,
                                             @Param("round") int round,
                                             Pageable pageable);

    // 위와 같되 AI가 손대지 않은 기사만 — 오염 없는 정답지를 만들 때
    @Query("SELECT a FROM Article a WHERE NOT EXISTS " +
           "(SELECT 1 FROM ArticleImportanceLabel l WHERE l.article = a " +
           " AND l.labeledBy = :labeledBy AND l.round = :round) " +
           "AND NOT EXISTS (SELECT 1 FROM ArticleImportanceLabel s WHERE s.article = a " +
           "                AND s.origin = com.example.neeews.article.domain.LabelOrigin.AI) " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findUnlabeledByWithoutAiLabel(@Param("labeledBy") String labeledBy,
                                                @Param("round") int round,
                                                Pageable pageable);

    // 한 기사에 라벨이 여러 건(AI·사람) 달릴 수 있으므로 DISTINCT 로 중복 행을 막는다
    @Query("SELECT DISTINCT a FROM Article a JOIN ArticleImportanceLabel l ON l.article = a " +
           "WHERE l.label = :label ORDER BY a.publishedAt DESC")
    Page<Article> findByLabel(@Param("label") Importance label, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.aiImportance IS NULL AND a.publishedAt >= :from " +
           "AND a.category IS NOT NULL ORDER BY a.publishedAt DESC")
    List<Article> findUnjudged(@Param("from") LocalDateTime from, Pageable pageable);

    @Query("SELECT DISTINCT a.category FROM Article a " +
           "WHERE a.aiImportance = com.example.neeews.article.domain.Importance.HIGH " +
           "AND a.publishedAt >= :from AND a.category IS NOT NULL")
    List<String> findHeadlineCategories(@Param("from") LocalDateTime from);

    @Query("SELECT a FROM Article a WHERE a.category = :category AND a.publishedAt >= :from " +
           "AND a.aiImportance = com.example.neeews.article.domain.Importance.HIGH " +
           "ORDER BY a.aiImportanceScore DESC, a.publishedAt DESC")
    List<Article> findHeadlines(@Param("category") String category,
                                @Param("from") LocalDateTime from,
                                Pageable pageable);

    long countByAiImportanceIsNullAndPublishedAtAfter(LocalDateTime from);
}
