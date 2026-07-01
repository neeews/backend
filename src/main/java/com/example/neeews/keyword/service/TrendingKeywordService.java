package com.example.neeews.keyword.service;

import com.example.neeews.keyword.domain.TrendingKeyword;
import com.example.neeews.keyword.domain.TrendingKeyword.KeywordChange;
import com.example.neeews.keyword.repository.TrendingKeywordRepository;
import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
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

    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
    private final TrendingKeywordRepository trendingKeywordRepository;
    private final ArticleRepository articleRepository;

    public TrendingKeywordService(TrendingKeywordRepository trendingKeywordRepository,
                                  ArticleRepository articleRepository) {
        this.trendingKeywordRepository = trendingKeywordRepository;
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

    // 매일 새벽 2시 갱신
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void refreshTrendingKeywords() {
        log.info("[키워드] 트렌딩 키워드 갱신 시작");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<TrendingKeyword> prevKeywords = trendingKeywordRepository.findByDateOrderByRankAsc(yesterday);
        Map<String, Integer> prevRankMap = prevKeywords.stream()
                .collect(Collectors.toMap(TrendingKeyword::getWord, TrendingKeyword::getRank));

        List<Article> recentArticles = articleRepository.findAll(
                PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "publishedAt"))
        ).getContent();

        Map<String, Long> wordCount = recentArticles.stream()
                .map(a -> a.getTitle() + " " + (a.getDescription() != null
                        ? HtmlUtils.htmlUnescape(a.getDescription().replaceAll("<[^>]*>", ""))
                        : ""))
                .flatMap(text -> komoran.analyze(text).getNouns().stream())
                .filter(w -> w.length() >= 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        List<Map.Entry<String, Long>> top10 = wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList();

        trendingKeywordRepository.deleteByDate(today);

        for (int i = 0; i < top10.size(); i++) {
            String word = top10.get(i).getKey();
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
