package com.example.neeews.admin.controller;

import com.example.neeews.suggestion.dto.request.SuggestionStatusRequest;
import com.example.neeews.suggestion.dto.response.SuggestionResponse;
import com.example.neeews.suggestion.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/suggestions")
@RequiredArgsConstructor
public class AdminSuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<SuggestionResponse>> getAll() {
        return ResponseEntity.ok(suggestionService.getAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SuggestionResponse> updateStatus(@PathVariable Long id,
                                                           @Valid @RequestBody SuggestionStatusRequest request) {
        return ResponseEntity.ok(suggestionService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        suggestionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
