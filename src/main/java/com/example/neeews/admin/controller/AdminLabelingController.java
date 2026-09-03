package com.example.neeews.admin.controller;

import com.example.neeews.article.dto.request.ImportanceLabelRequest;
import com.example.neeews.article.dto.request.SeedFilter;
import com.example.neeews.article.dto.response.ImportanceLabelResponse;
import com.example.neeews.article.dto.response.LabelingArticleResponse;
import com.example.neeews.article.service.ArticleImportanceLabelService;
import com.example.neeews.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/labeling")
@RequiredArgsConstructor
public class AdminLabelingController {

    private final ArticleImportanceLabelService labelService;

    @GetMapping("/next")
    public ResponseEntity<Map<String, Object>> next(Authentication authentication,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "0") int round,
                                                    @RequestParam(defaultValue = "ALL") SeedFilter filter) {
        String labeledBy = requireLabeler(authentication);
        List<LabelingArticleResponse> articles = labelService.findNext(labeledBy, round, size, filter);
        return ResponseEntity.ok(Map.of(
                "articles", articles,
                "filter", filter,
                "round", round
        ));
    }

    @PostMapping("/{articleId}")
    public ResponseEntity<ImportanceLabelResponse> label(Authentication authentication,
                                                         @PathVariable Long articleId,
                                                         @Valid @RequestBody ImportanceLabelRequest request) {
        String labeledBy = requireLabeler(authentication);
        return ResponseEntity.ok(
                labelService.label(articleId, request.getLabel(), labeledBy, request.getRound()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(Authentication authentication) {
        return ResponseEntity.ok(labelService.stats(requireLabeler(authentication)));
    }

    // 라벨을 누가 매겼는지가 이 기능의 핵심이라, 라벨러를 특정하지 못하면 저장 자체를 막는다.
    private String requireLabeler(Authentication authentication) {
        String email = AuthUtils.resolveEmail(authentication);
        if (email == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return email;
    }
}
