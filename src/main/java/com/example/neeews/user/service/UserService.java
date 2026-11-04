package com.example.neeews.user.service;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.articleread.service.ArticleReadService;
import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.dto.response.UserResponse;
import com.example.neeews.auth.repository.RefreshTokenRepository;
import com.example.neeews.auth.repository.UserRepository;
import com.example.neeews.bookmark.repository.BookmarkRepository;
import com.example.neeews.search.repository.SearchHistoryRepository;
import com.example.neeews.suggestion.service.SuggestionService;
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
    private final ArticleReadService articleReadService;
    private final SuggestionService suggestionService;

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
        articleReadService.clearHistoryForUser(email);
        suggestionService.deleteAllByUser(email);
        bookmarkRepository.deleteAllByUser(user);
        refreshTokenRepository.deleteByUser(user);
        searchHistoryRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    @Transactional
    public boolean updateBriefingSubscription(String email, boolean subscribed) {
        User user = getUser(email);
        if (subscribed) {
            user.subscribeBriefing();
        } else {
            user.unsubscribeBriefing();
        }
        return user.isBriefingSubscribed();
    }

    // 메일 링크는 로그인 없이 누른다. 없는 토큰이어도 똑같이 끝내, 토큰이 유효한지 떠보는 데 쓰이지 않게 한다.
    @Transactional
    public void unsubscribeBriefing(String token) {
        userRepository.findByBriefingToken(token).ifPresent(User::unsubscribeBriefing);
    }

    @Transactional(readOnly = true)
    public List<BriefingRecipient> getBriefingRecipients() {
        return userRepository.findAllByBriefingSubscribedTrue().stream()
                .map(user -> new BriefingRecipient(user.getEmail(), user.getBriefingToken()))
                .toList();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    public record BriefingRecipient(String email, String unsubscribeToken) {}
}
