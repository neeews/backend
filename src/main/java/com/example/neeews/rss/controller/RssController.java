package com.example.neeews.rss.controller;

import com.example.neeews.rss.domain.NewsSource;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rss")
@RequiredArgsConstructor
public class RssController {

    private final RssFetchService rssFetchService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> sync() {
        return ResponseEntity.ok(Map.of("saved", rssFetchService.fetchAll()));
    }

    @PostMapping("/sync/{source}")
    public ResponseEntity<Map<String, Integer>> syncSource(@PathVariable String source) {
        NewsSource newsSource = NewsSource.valueOf(source.toUpperCase());
        return ResponseEntity.ok(Map.of("saved", rssFetchService.fetchSource(newsSource)));
    }

    @PostMapping("/normalize-categories")
    public ResponseEntity<Map<String, Integer>> normalizeCategories() {
        return ResponseEntity.ok(Map.of("updated", rssFetchService.normalizeCategories()));
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> categoryStats() {
        return ResponseEntity.ok(Map.of("categories", rssFetchService.getCategoryStats()));
    }
}
