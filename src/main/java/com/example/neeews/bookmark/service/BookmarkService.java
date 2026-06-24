package com.example.neeews.bookmark.service;

import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.repository.UserRepository;
import com.example.neeews.bookmark.domain.Bookmark;
import com.example.neeews.bookmark.repository.BookmarkRepository;
import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public void addBookmark(Long articleId, String email) {
        User user = getUser(email);
        Article article = getArticle(articleId);
        if (bookmarkRepository.existsByUserAndArticle(user, article)) {
            return;
        }
        bookmarkRepository.save(Bookmark.builder().user(user).article(article).build());
    }

    @Transactional
    public void removeBookmark(Long articleId, String email) {
        User user = getUser(email);
        Article article = getArticle(articleId);
        bookmarkRepository.findByUserAndArticle(user, article)
                .ifPresent(bookmarkRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long articleId, String email) {
        if (email == null) return false;
        User user = getUser(email);
        Article article = getArticle(articleId);
        return bookmarkRepository.existsByUserAndArticle(user, article);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Article getArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));
    }
}
