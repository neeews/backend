package com.example.neeews.search.repository;

import com.example.neeews.auth.domain.User;
import com.example.neeews.search.domain.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findTop5ByUserOrderBySearchedAtDesc(User user);
    void deleteAllByUser(User user);
}
