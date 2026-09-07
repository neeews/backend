package com.example.neeews.rss.scheduler;

import com.example.neeews.rss.event.ArticlesFetchedEvent;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssScheduler {

    private final RssFetchService rssFetchService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void fetchRss() {
        log.info("[RSS 스케줄러] RSS 수집 시작");
        int total = rssFetchService.fetchAll();
        log.info("[RSS 스케줄러] 완료 - 총 {}건 저장", total);

        // 중요도 판정 서비스가 RssFetchService를 이미 주입받고 있어 직접 호출하면 순환 의존이 된다.
        // 이벤트로 알리면 의존 방향이 한쪽으로만 흐른다.
        if (total > 0) {
            eventPublisher.publishEvent(new ArticlesFetchedEvent(total));
        }
    }
}
