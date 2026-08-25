package com.example.neeews.user.controller;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.articleread.service.ArticleReadService;
import com.example.neeews.auth.dto.response.UserResponse;
import com.example.neeews.search.service.SearchHistoryService;
import com.example.neeews.security.AuthUtils;
import com.example.neeews.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SearchHistoryService searchHistoryService;
    private final ArticleReadService articleReadService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getBookmarks(Authentication authentication) {
        List<ArticleResponse> articles = userService.getBookmarkedArticles(authentication.getName());
        return ResponseEntity.ok(Map.of("articles", articles));
    }

    @GetMapping("/me/history")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getReadHistory(Authentication authentication) {
        List<ArticleResponse> articles = articleReadService.getReadHistory(AuthUtils.resolveEmail(authentication));
        return ResponseEntity.ok(Map.of("articles", articles));
    }

    @DeleteMapping("/me/history")
    public ResponseEntity<Void> clearReadHistory(Authentication authentication) {
        articleReadService.clearHistoryForUser(AuthUtils.resolveEmail(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/history/{articleId}")
    public ResponseEntity<Void> removeReadHistory(@PathVariable Long articleId, Authentication authentication) {
        articleReadService.removeFromHistory(articleId, AuthUtils.resolveEmail(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/search-history")
    public ResponseEntity<Map<String, List<String>>> getSearchHistory(Authentication authentication) {
        return ResponseEntity.ok(Map.of("history", searchHistoryService.getRecentQueries(authentication.getName())));
    }

    @DeleteMapping("/me/search-history")
    public ResponseEntity<Void> clearSearchHistory(Authentication authentication) {
        searchHistoryService.clearHistory(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        userService.deleteAccount(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
