package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.TopicMentionCount;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.article.repository.TopicMentionCountRepository;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 특정 시점에 평소보다 언급량이 급증하는 주제(태풍, 폭우, 월드컵 등)를 감지해
// ArticleService가 "핫이슈" 기사를 고를 때 쓰는 기준으로 제공한다.
// 급증 주제가 없으면 빈 리스트를 반환하고, 이 경우 ArticleService는 조회수 기반 방식으로 대체한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class HotTopicService {

    private static final Set<String> STOP_WORDS = Set.of(
            "기자", "뉴스", "보도", "기사", "관련", "이번", "지난", "오늘", "내일", "올해",
            "대해", "통해", "위해", "것으로", "경우", "사실", "가운데", "현재", "이후", "당시",
            "밝혔다", "있다", "했다", "된다", "이다", "연합뉴스"
    );

    private static final int HISTORY_DAYS = 7;
    private static final double SURGE_SMOOTHING = 3.0;
    private static final double SURGE_THRESHOLD = 2.5;
    private static final long MIN_MENTIONS = 5;
    private static final int MAX_HOT_TOPICS = 3;

    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
    private final ArticleRepository articleRepository;
    private final TopicMentionCountRepository topicMentionCountRepository;

    private volatile List<String> currentHotTopics = List.of();

    public List<String> getCurrentHotTopics() {
        return currentHotTopics;
    }

    // 매시 정각 갱신: 이번 시간에 새로 올라온 기사에서 명사를 뽑아 오늘 누적 언급량에 더하고,
    // 최근 7일 평소 언급량 대비 오늘 언급량이 기준치 이상 튀는 단어들을 점수순 상위 MAX_HOT_TOPICS개까지 핫이슈 주제로 저장한다.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void refreshHotTopic() {
        log.info("[핫이슈] 급상승 주제 갱신 시작");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Article> hourlyArticles = articleRepository.findByPublishedAtBetween(now.minusHours(1), now);

        Map<String, Long> hourlyWordCount = hourlyArticles.stream()
                .map(a -> a.getTitle() + " " + (a.getDescription() != null
                        ? HtmlUtils.htmlUnescape(a.getDescription().replaceAll("<[^>]*>", ""))
                        : ""))
                .flatMap(text -> komoran.analyze(text).getNouns().stream())
                .filter(w -> w.length() >= 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        Map<String, TopicMentionCount> todayCounts = topicMentionCountRepository.findByDate(today).stream()
                .collect(Collectors.toMap(TopicMentionCount::getWord, c -> c));

        hourlyWordCount.forEach((word, count) -> {
            TopicMentionCount existing = todayCounts.get(word);
            if (existing != null) {
                existing.addCount(count);
            } else {
                todayCounts.put(word, TopicMentionCount.builder()
                        .date(today).word(word).count(count).build());
            }
        });
        topicMentionCountRepository.saveAll(todayCounts.values());

        Map<String, Long> historicalTotals = topicMentionCountRepository
                .findByWordInAndDateBetween(todayCounts.keySet(), today.minusDays(HISTORY_DAYS), today.minusDays(1)).stream()
                .collect(Collectors.groupingBy(TopicMentionCount::getWord, Collectors.summingLong(TopicMentionCount::getCount)));

        double dayProgress = (now.getHour() + 1) / 24.0;

        currentHotTopics = todayCounts.values().stream()
                .filter(c -> c.getCount() >= MIN_MENTIONS)
                .map(c -> {
                    double avgDaily = historicalTotals.getOrDefault(c.getWord(), 0L) / (double) HISTORY_DAYS;
                    double expectedByNow = avgDaily * dayProgress;
                    double score = (c.getCount() + SURGE_SMOOTHING) / (expectedByNow + SURGE_SMOOTHING);
                    return Map.entry(c.getWord(), score);
                })
                .filter(e -> e.getValue() >= SURGE_THRESHOLD)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_HOT_TOPICS)
                .map(Map.Entry::getKey)
                .toList();
        log.info("[핫이슈] 급상승 주제 갱신 완료: {}", currentHotTopics);
    }
}
