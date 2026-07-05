package com.example.neeews.article.controller;

import com.example.neeews.article.dto.response.ArticleDetailResponse;
import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        Page<ArticleResponse> result = articleService.getArticlesByCategory(category, sort, page, email);
        return ResponseEntity.ok(Map.of(
                "articles", result.getContent(),
                "total", result.getTotalElements(),
                "page", page
        ));
    }

    @GetMapping("/breaking")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getBreaking(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(Map.of("articles", articleService.getBreakingArticles(email)));
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getLatest(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(Map.of("articles", articleService.getLatestArticles(email)));
    }

    @GetMapping("/hot")
    public ResponseEntity<Map<String, List<ArticleResponse>>> getHot(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(Map.of("articles", articleService.getHotArticles(email)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(articleService.getArticleDetail(id, email));
    }

}
