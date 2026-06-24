package com.example.neeews.search.controller;

import com.example.neeews.article.dto.ArticleResponse;
import com.example.neeews.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<ArticleResponse> result = articleService.searchArticles(q, PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(Map.of(
                "total", result.getTotalElements(),
                "articles", result.getContent()
        ));
    }
}
