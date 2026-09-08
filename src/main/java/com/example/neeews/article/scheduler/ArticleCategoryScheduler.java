package com.example.neeews.article.scheduler;

import com.example.neeews.article.service.ArticleCategoryAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.category.enabled", havingValue = "true")
public class ArticleCategoryScheduler {

    private final ArticleCategoryAiService articleCategoryAiService;

    // 중요도 판정과 달리 RSS 수집 이벤트에 붙이지 않는다. 기사 한 건에 ollama 호출이 한 번씩 들어가
    // 수집 스레드를 몇 분씩 붙잡게 된다. 수집(10분 주기)과 어긋나도록 5분 오프셋을 둔다.
    @Scheduled(cron = "0 5/10 * * * *")
    public void classify() {
        try {
            articleCategoryAiService.classifyBatch();
        } catch (Exception e) {
            log.error("[카테고리] 배치 실행 실패", e);
        }
    }
}
