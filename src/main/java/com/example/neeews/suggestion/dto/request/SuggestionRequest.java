package com.example.neeews.suggestion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SuggestionRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
