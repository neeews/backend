package com.example.neeews.suggestion.repository;

import com.example.neeews.suggestion.domain.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    List<Suggestion> findAllByOrderByCreatedAtDesc();
}
