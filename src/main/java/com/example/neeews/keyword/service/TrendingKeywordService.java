package com.example.neeews.keyword.service;

import com.example.neeews.keyword.domain.DailyKeywordCount;
import com.example.neeews.keyword.domain.TrendingKeyword;
import com.example.neeews.keyword.domain.TrendingKeyword.KeywordChange;
import com.example.neeews.keyword.repository.DailyKeywordCountRepository;
import com.example.neeews.keyword.repository.TrendingKeywordRepository;
import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TrendingKeywordService {

    private static final Set<String> STOP_WORDS = Set.of(
            "기자", "뉴스", "보도", "기사", "관련", "이번", "지난", "오늘", "내일", "올해",
            "대해", "통해", "위해", "것으로", "경우", "사실", "가운데", "현재", "이후", "당시",
            "밝혔다", "있다", "했다", "된다", "이다"
    );

    private static final int HISTORY_DAYS = 7;
    private static final double SURGE_SMOOTHING = 3.0;

    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
    private final TrendingKeywordRepository trendingKeywordRepository;
    private final DailyKeywordCountRepository dailyKeywordCountRepository;
    private final ArticleRepository articleRepository;

    public TrendingKeywordService(TrendingKeywordRepository trendingKeywordRepository,
                                  DailyKeywordCountRepository dailyKeywordCountRepository,
                                  ArticleRepository articleRepository) {
        this.trendingKeywordRepository = trendingKeywordRepository;
        this.dailyKeywordCountRepository = dailyKeywordCountRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<TrendingKeyword> getTodayKeywords() {
        List<TrendingKeyword> keywords = trendingKeywordRepository.findByDateOrderByRankAsc(LocalDate.now());
        if (keywords.isEmpty()) {
            keywords = trendingKeywordRepository.findByDateOrderByRankAsc(LocalDate.now().minusDays(1));
        }
        return keywords;
    }

    // 매시 정각 갱신: 이번 시간에 새로 올라온 기사에서 키워드를 수집해 오늘 누적 카운트에 더하고,
    // 최근 7일간의 평소 언급량 대비 급상승 정도(surge score)를 기준으로 상위 10개를 다시 뽑아 표시용 순위를 갱신한다.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void refreshTrendingKeywords() {
        log.info("[키워드] 트렌딩 키워드 갱신 시작");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
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

        Map<String, DailyKeywordCount> todayCounts = dailyKeywordCountRepository.findByDate(today).stream()
                .collect(Collectors.toMap(DailyKeywordCount::getWord, c -> c));

        hourlyWordCount.forEach((word, count) -> {
            DailyKeywordCount existing = todayCounts.get(word);
            if (existing != null) {
                existing.addCount(count);
            } else {
                todayCounts.put(word, DailyKeywordCount.builder()
                        .date(today).word(word).count(count).build());
            }
        });
        dailyKeywordCountRepository.saveAll(todayCounts.values());

        List<TrendingKeyword> prevKeywords = trendingKeywordRepository.findByDateOrderByRankAsc(yesterday);
        Map<String, Integer> prevRankMap = prevKeywords.stream()
                .collect(Collectors.toMap(TrendingKeyword::getWord, TrendingKeyword::getRank));

        Map<String, Long> historicalTotals = dailyKeywordCountRepository
                .findByWordInAndDateBetween(todayCounts.keySet(), today.minusDays(HISTORY_DAYS), yesterday).stream()
                .collect(Collectors.groupingBy(DailyKeywordCount::getWord, Collectors.summingLong(DailyKeywordCount::getCount)));

        double dayProgress = (now.getHour() + 1) / 24.0;

        List<DailyKeywordCount> top10 = todayCounts.values().stream()
                .sorted(Comparator.comparingDouble((DailyKeywordCount c) -> {
                    double avgDaily = historicalTotals.getOrDefault(c.getWord(), 0L) / (double) HISTORY_DAYS;
                    double expectedByNow = avgDaily * dayProgress;
                    return (c.getCount() + SURGE_SMOOTHING) / (expectedByNow + SURGE_SMOOTHING);
                }).reversed())
                .limit(10)
                .toList();

        trendingKeywordRepository.deleteByDate(today);

        for (int i = 0; i < top10.size(); i++) {
            String word = top10.get(i).getWord();
            int rank = i + 1;
            Integer prevRank = prevRankMap.get(word);

            KeywordChange change;
            if (prevRank == null) change = KeywordChange.NEW;
            else if (rank < prevRank) change = KeywordChange.up;
            else if (rank > prevRank) change = KeywordChange.down;
            else change = KeywordChange.same;

            trendingKeywordRepository.save(TrendingKeyword.builder()
                    .rank(rank).word(word).change(change).date(today).build());
        }
        log.info("[키워드] 트렌딩 키워드 갱신 완료");
    }
}
