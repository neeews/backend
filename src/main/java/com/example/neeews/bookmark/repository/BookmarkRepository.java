package com.example.neeews.bookmark.repository;

import com.example.neeews.auth.domain.User;
import com.example.neeews.bookmark.domain.Bookmark;
import com.example.neeews.article.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserAndArticle(User user, Article article);

    Optional<Bookmark> findByUserAndArticle(User user, Article article);

    List<Bookmark> findAllByUserOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);
}
