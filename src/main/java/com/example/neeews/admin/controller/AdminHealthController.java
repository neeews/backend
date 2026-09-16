package com.example.neeews.admin.controller;

import com.example.neeews.article.dto.response.PipelineStatusResponse;
import com.example.neeews.article.service.ArticlePipelineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {

    private final ArticlePipelineStatusService articlePipelineStatusService;

    @GetMapping
    public ResponseEntity<PipelineStatusResponse> getHealth() {
        return ResponseEntity.ok(articlePipelineStatusService.getStatus());
    }
}
