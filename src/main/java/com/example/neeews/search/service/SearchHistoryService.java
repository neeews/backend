package com.example.neeews.search.service;

import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.repository.UserRepository;
import com.example.neeews.search.domain.SearchHistory;
import com.example.neeews.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void save(String email, String query) {
        User user = userRepository.findByEmail(email).orElseThrow();
        searchHistoryRepository.save(SearchHistory.builder()
                .user(user)
                .query(query)
                .build());
    }

    @Transactional(readOnly = true)
    public List<String> getRecentQueries(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return searchHistoryRepository.findTop5ByUserOrderBySearchedAtDesc(user)
                .stream()
                .map(SearchHistory::getQuery)
                .toList();
    }

    @Transactional
    public void clearHistory(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        searchHistoryRepository.deleteAllByUser(user);
    }
}
