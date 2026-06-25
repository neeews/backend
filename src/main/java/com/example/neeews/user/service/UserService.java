package com.example.neeews.user.service;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.dto.response.UserResponse;
import com.example.neeews.auth.repository.RefreshTokenRepository;
import com.example.neeews.auth.repository.UserRepository;
import com.example.neeews.bookmark.repository.BookmarkRepository;
import com.example.neeews.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = getUser(email);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBookmarkedArticles(String email) {
        User user = getUser(email);
        return bookmarkRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(b -> ArticleResponse.from(b.getArticle()))
                .toList();
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = getUser(email);
        bookmarkRepository.deleteAllByUser(user);
        refreshTokenRepository.deleteByUser(user);
        searchHistoryRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
