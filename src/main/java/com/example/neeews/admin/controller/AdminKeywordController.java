package com.example.neeews.admin.controller;

import com.example.neeews.keyword.service.TrendingKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/keywords")
@RequiredArgsConstructor
public class AdminKeywordController {

    private final TrendingKeywordService trendingKeywordService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshTrending() {
        trendingKeywordService.refreshTrendingKeywords();
        return ResponseEntity.ok().build();
    }
}
