package com.example.neeews.keyword.controller;

import com.example.neeews.keyword.dto.response.TrendingKeywordResponse;
import com.example.neeews.keyword.service.TrendingKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final TrendingKeywordService trendingKeywordService;

    @GetMapping("/trending")
    public ResponseEntity<Map<String, List<TrendingKeywordResponse>>> getTrending() {
        List<TrendingKeywordResponse> keywords = trendingKeywordService.getTodayKeywords()
                .stream().map(TrendingKeywordResponse::from).toList();
        return ResponseEntity.ok(Map.of("keywords", keywords));
    }

}
