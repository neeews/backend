package com.example.neeews.article.scheduler;

import com.example.neeews.article.service.ArticleImportanceAiService;
import com.example.neeews.rss.event.ArticlesFetchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.importance.enabled", havingValue = "true")
public class ArticleImportanceScheduler {

    private final ArticleImportanceAiService articleImportanceAiService;

    // RSS 수집이 끝나면 곧바로 새 기사를 판정한다. 이벤트는 동기라 수집 스레드에서 이어 실행된다.
    @EventListener
    public void onArticlesFetched(ArticlesFetchedEvent event) {
        log.info("[주요기사] 신규 {}건 수집 직후 판정 시작", event.savedCount());
        judge();
    }

    // muni 호출이 실패한 기사와 한 번에 못 끝낸 밀린 기사를 이어서 처리한다.
    @Scheduled(cron = "0 5/30 * * * *")
    public void judge() {
        try {
            articleImportanceAiService.judgeBatch();
        } catch (Exception e) {
            log.error("[주요기사] 배치 실행 실패", e);
        }
    }
}
