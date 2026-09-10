package com.example.neeews.article.scheduler;

import com.example.neeews.article.service.ArticleSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.summary.enabled", havingValue = "true")
public class ArticleSummaryScheduler {

    private final ArticleSummaryService articleSummaryService;

    // 오늘의 뉴스는 하루가 지나면서 채워지는 목록이라 매시간 돌린다.
    // 하루 두 번 5건씩으로는 그날 중요 기사가 요약되기 전에 목록이 비어 보인다.
    @Scheduled(cron = "0 10 * * * *")
    public void summarize() {
        try {
            articleSummaryService.summarizeBatch();
        } catch (Exception e) {
            log.error("[요약] 배치 실행 실패", e);
        }
    }
}
