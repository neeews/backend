package com.example.neeews.suggestion.controller;

import com.example.neeews.suggestion.dto.request.SuggestionRequest;
import com.example.neeews.suggestion.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<Void> submit(@Valid @RequestBody SuggestionRequest request,
                                       Authentication authentication) {
        suggestionService.submit(request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
