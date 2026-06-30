package com.example.neeews.suggestion.dto.request;

import com.example.neeews.suggestion.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SuggestionStatusRequest {

    @NotNull
    private SuggestionStatus status;
}
