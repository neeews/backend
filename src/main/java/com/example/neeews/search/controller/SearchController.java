package com.example.neeews.search.controller;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.service.ArticleService;
import com.example.neeews.search.service.SearchHistoryService;
import com.example.neeews.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    // 상한이 없으면 limit=100000 한 번으로 수만 건을 읽어 응답을 만든다. 화면 한 페이지에 충분한 크기로 막는다.
    private static final int MAX_LIMIT = 100;

    private final ArticleService articleService;
    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String q,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> sources,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        if (page < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("한 번에 조회할 수 있는 검색 결과는 1~" + MAX_LIMIT + "건입니다.");
        }
        String email = AuthUtils.resolveEmail(authentication);
        Page<ArticleResponse> result = articleService.searchArticles(q, categories, sources, PageRequest.of(page - 1, limit), email);
        if (email != null) {
            searchHistoryService.save(email, q);
        }
        return ResponseEntity.ok(Map.of(
                "total", result.getTotalElements(),
                "articles", result.getContent()
        ));
    }
}
