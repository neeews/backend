package com.example.neeews.suggestion.dto.response;

import com.example.neeews.suggestion.domain.Suggestion;
import com.example.neeews.suggestion.domain.SuggestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SuggestionResponse {

    private Long id;
    private String title;
    private String content;
    private String userEmail;
    private SuggestionStatus status;
    private LocalDateTime createdAt;

    public static SuggestionResponse from(Suggestion suggestion) {
        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .title(suggestion.getTitle())
                .content(suggestion.getContent())
                .userEmail(suggestion.getUserEmail())
                .status(suggestion.getStatus())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}
