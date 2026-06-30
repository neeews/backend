package com.example.neeews.search.controller;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.service.ArticleService;
import com.example.neeews.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ArticleService articleService;
    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String q,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        Page<ArticleResponse> result = articleService.searchArticles(q, filter, PageRequest.of(page - 1, limit));
        if (authentication != null) {
            searchHistoryService.save(authentication.getName(), q);
        }
        return ResponseEntity.ok(Map.of(
                "total", result.getTotalElements(),
                "articles", result.getContent()
        ));
    }
}
