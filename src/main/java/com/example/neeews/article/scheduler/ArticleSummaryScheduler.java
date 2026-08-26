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

    @Scheduled(cron = "0 10 0,12 * * *")
    public void summarize() {
        try {
            articleSummaryService.summarizeBatch();
        } catch (Exception e) {
            log.error("[요약] 배치 실행 실패", e);
        }
    }
}
