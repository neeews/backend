package com.example.neeews.articleread.repository;

import com.example.neeews.articleread.domain.ArticleRead;
import com.example.neeews.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleReadRepository extends JpaRepository<ArticleRead, Long> {

    boolean existsByUserAndArticleId(User user, Long articleId);

    @Query("SELECT ar.article.id FROM ArticleRead ar WHERE ar.user = :user AND ar.article.id IN :articleIds")
    List<Long> findReadArticleIds(@Param("user") User user, @Param("articleIds") List<Long> articleIds);
}
