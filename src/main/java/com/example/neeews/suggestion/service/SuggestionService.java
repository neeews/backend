package com.example.neeews.suggestion.service;

import com.example.neeews.suggestion.domain.Suggestion;
import com.example.neeews.suggestion.domain.SuggestionStatus;
import com.example.neeews.suggestion.dto.request.SuggestionRequest;
import com.example.neeews.suggestion.dto.response.SuggestionResponse;
import com.example.neeews.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    @Transactional
    public void submit(SuggestionRequest request, String userEmail) {
        suggestionRepository.save(Suggestion.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .userEmail(userEmail)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> getAll() {
        return suggestionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SuggestionResponse::from)
                .toList();
    }

    @Transactional
    public SuggestionResponse updateStatus(Long id, SuggestionStatus status) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("건의사항을 찾을 수 없습니다."));
        suggestion.updateStatus(status);
        return SuggestionResponse.from(suggestion);
    }

    @Transactional
    public void delete(Long id) {
        if (!suggestionRepository.existsById(id)) {
            throw new IllegalArgumentException("건의사항을 찾을 수 없습니다.");
        }
        suggestionRepository.deleteById(id);
    }
}
