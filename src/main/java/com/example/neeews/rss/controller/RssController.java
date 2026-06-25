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

    @PostMapping("/fill-images")
    public ResponseEntity<Map<String, Integer>> fillImages() {
        return ResponseEntity.ok(Map.of("updated", rssFetchService.fillMissingImageUrls()));
    }

    @PostMapping("/fill-images/web")
    public ResponseEntity<Map<String, Integer>> fillImagesFromWeb() {
        return ResponseEntity.ok(Map.of("updated", rssFetchService.fillMissingImageUrlsFromWeb()));
    }

    @PostMapping("/enrich-descriptions")
    public ResponseEntity<Map<String, Integer>> enrichDescriptions() {
        return ResponseEntity.ok(Map.of("updated", rssFetchService.enrichDescriptionsFromWeb()));
    }
}
