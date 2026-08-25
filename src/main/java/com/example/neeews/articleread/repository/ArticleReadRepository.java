package com.example.neeews.articleread.repository;

import com.example.neeews.articleread.domain.ArticleRead;
import com.example.neeews.auth.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleReadRepository extends JpaRepository<ArticleRead, Long> {

    Optional<ArticleRead> findByUserAndArticleId(User user, Long articleId);

    @Query("SELECT ar.article.id FROM ArticleRead ar WHERE ar.user = :user AND ar.article.id IN :articleIds")
    List<Long> findReadArticleIds(@Param("user") User user, @Param("articleIds") List<Long> articleIds);

    @Query("SELECT ar FROM ArticleRead ar JOIN FETCH ar.article WHERE ar.user = :user ORDER BY ar.lastReadAt DESC")
    List<ArticleRead> findRecentWithArticle(@Param("user") User user, Pageable pageable);

    void deleteAllByUser(User user);

    void deleteByUserAndArticleId(User user, Long articleId);
}
