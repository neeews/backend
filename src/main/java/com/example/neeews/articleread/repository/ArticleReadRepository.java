package com.example.neeews.articleread.repository;

import com.example.neeews.articleread.domain.ArticleRead;
import com.example.neeews.auth.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleReadRepository extends JpaRepository<ArticleRead, Long> {

    // JPQL엔 upsert가 없다. 조회 후 저장하면 같은 기사를 동시에 처음 연 요청끼리 유니크 제약에 걸려 상세 조회가 500이 된다.
    @Modifying
    @Query(value = "INSERT INTO article_reads (user_id, article_id, created_at, last_read_at) " +
                   "VALUES (:userId, :articleId, :now, :now) " +
                   "ON DUPLICATE KEY UPDATE last_read_at = :now",
           nativeQuery = true)
    void upsertRead(@Param("userId") Long userId, @Param("articleId") Long articleId, @Param("now") LocalDateTime now);

    @Query("SELECT ar.article.id FROM ArticleRead ar WHERE ar.user = :user AND ar.article.id IN :articleIds")
    List<Long> findReadArticleIds(@Param("user") User user, @Param("articleIds") List<Long> articleIds);

    @Query("SELECT ar FROM ArticleRead ar JOIN FETCH ar.article WHERE ar.user = :user ORDER BY ar.lastReadAt DESC")
    List<ArticleRead> findRecentWithArticle(@Param("user") User user, Pageable pageable);

    // 파생 delete는 엔티티를 읽어 한 건씩 지워, 동시에 온 삭제 요청 중 늦은 쪽이 "0건 삭제" 예외로 500이 된다.
    // 한 문장으로 지우면 이미 지워진 행은 그냥 0건으로 끝난다.
    @Modifying
    @Query("DELETE FROM ArticleRead ar WHERE ar.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM ArticleRead ar WHERE ar.user = :user AND ar.article.id = :articleId")
    void deleteByUserAndArticleId(@Param("user") User user, @Param("articleId") Long articleId);
}
