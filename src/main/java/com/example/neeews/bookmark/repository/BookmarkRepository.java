package com.example.neeews.bookmark.repository;

import com.example.neeews.auth.domain.User;
import com.example.neeews.bookmark.domain.Bookmark;
import com.example.neeews.article.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserAndArticle(User user, Article article);

    // JPQL엔 upsert가 없다. 있는지 보고 저장하면 더블클릭처럼 동시에 온 요청끼리 유니크 제약에 걸려 500이 된다.
    // INSERT IGNORE는 외래키 위반 같은 다른 오류까지 경고로 삼켜서, 중복일 때만 아무것도 안 하도록 쓴다.
    @Modifying
    @Query(value = "INSERT INTO bookmarks (user_id, article_id, created_at) VALUES (:userId, :articleId, :now) " +
                   "ON DUPLICATE KEY UPDATE id = id",
           nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("articleId") Long articleId, @Param("now") LocalDateTime now);

    // 조회 후 delete()로 지우면 동시에 온 삭제 요청 중 늦은 쪽이 "0건 삭제" 예외로 500이 된다.
    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user = :user AND b.article = :article")
    void deleteByUserAndArticle(@Param("user") User user, @Param("article") Article article);

    List<Bookmark> findAllByUserOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);
}
