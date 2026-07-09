package com.example.neeews.article.repository;

import com.example.neeews.article.domain.TopicMentionCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TopicMentionCountRepository extends JpaRepository<TopicMentionCount, Long> {

    List<TopicMentionCount> findByDate(LocalDate date);

    List<TopicMentionCount> findByWordInAndDateBetween(Collection<String> words, LocalDate start, LocalDate end);
}
