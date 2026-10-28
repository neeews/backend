package com.example.neeews.huggingface.controller;

import com.example.neeews.huggingface.dto.response.ModelResponse;
import com.example.neeews.huggingface.service.ModelReleaseService;
import com.example.neeews.huggingface.service.TrendingModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelReleaseService modelReleaseService;
    private final TrendingModelService trendingModelService;

    @GetMapping("/releases")
    public ResponseEntity<Map<String, Object>> getReleases(@RequestParam(defaultValue = "1") int page) {
        Page<ModelResponse> result = modelReleaseService.getReleases(page);
        return ResponseEntity.ok(Map.of(
                "models", result.getContent(),
                "total", result.getTotalElements(),
                "page", page
        ));
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, List<ModelResponse>>> getTrending() {
        return ResponseEntity.ok(Map.of("models", trendingModelService.getTrending()));
    }
}
