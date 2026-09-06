package com.example.neeews.article.scheduler;

import com.example.neeews.article.service.ArticleImportanceAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.importance.enabled", havingValue = "true")
public class ArticleImportanceScheduler {

    private final ArticleImportanceAiService articleImportanceAiService;

    // 하루 기사가 1500건대라 한 번에 못 끝낸다. 30분마다 돌며 미판정분을 이어서 처리한다.
    @Scheduled(cron = "0 5/30 * * * *")
    public void judge() {
        try {
            articleImportanceAiService.judgeBatch();
        } catch (Exception e) {
            log.error("[주요기사] 배치 실행 실패", e);
        }
    }
}
