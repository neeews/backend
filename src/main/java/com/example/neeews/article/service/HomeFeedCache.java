package com.example.neeews.article.service;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.dto.response.DailySummaryResponse;
import com.example.neeews.article.dto.response.HeadlineSectionResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

// 메인 화면 API는 비로그인이면 누가 불러도 같은 결과인데, 요청마다 DB 조회와 사건 중복 계산을 반복했다.
// 짧게 캐시해 같은 계산을 한 번만 하고, 만료 순간에 몰린 요청도 한 번의 계산을 함께 기다린다.
// 읽음 표시는 사용자마다 달라 캐시하지 않고, 서비스에서 캐시된 목록 위에 덧씌운다.
@Component
public class HomeFeedCache {

    // RSS 수집 주기(10분)보다 훨씬 짧아 새 기사 반영이 체감되지 않는다.
    private static final Duration FEED_TTL = Duration.ofSeconds(60);
    // 요약 배치가 매시간 도는 목록이고 계산이 가장 무거워 더 길게 둔다.
    private static final Duration DAILY_SUMMARY_TTL = Duration.ofMinutes(5);
    private static final int MAX_ENTRIES = 16;

    private final Cache<String, List<ArticleResponse>> articleLists = newCache(FEED_TTL);
    private final Cache<String, List<HeadlineSectionResponse>> headlines = newCache(FEED_TTL);
    private final Cache<String, List<DailySummaryResponse>> dailySummaries = newCache(DAILY_SUMMARY_TTL);

    public List<ArticleResponse> articles(String key, Supplier<List<ArticleResponse>> loader) {
        return articleLists.get(key, k -> loader.get());
    }

    public List<HeadlineSectionResponse> headlines(Supplier<List<HeadlineSectionResponse>> loader) {
        return headlines.get("headlines", k -> loader.get());
    }

    public List<DailySummaryResponse> dailySummaries(Supplier<List<DailySummaryResponse>> loader) {
        return dailySummaries.get("today", k -> loader.get());
    }

    private static <T> Cache<String, T> newCache(Duration ttl) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(MAX_ENTRIES).build();
    }
}
