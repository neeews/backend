package com.example.neeews.user.controller;

import com.example.neeews.article.dto.ArticleResponse;
import com.example.neeews.auth.dto.UserResponse;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getBookmarks(Authentication authentication) {
        List<ArticleResponse> articles = userService.getBookmarkedArticles(authentication.getName());
        return ResponseEntity.ok(Map.of("articles", articles));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        userService.deleteAccount(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
