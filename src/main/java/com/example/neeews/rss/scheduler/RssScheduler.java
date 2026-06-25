package com.example.neeews.rss.scheduler;

import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssScheduler {

    private final RssFetchService rssFetchService;

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void fetchRss() {
        log.info("[RSS 스케줄러] RSS 수집 시작");
        int total = rssFetchService.fetchAll();
        log.info("[RSS 스케줄러] 완료 - 총 {}건 저장", total);

        log.info("[RSS 스케줄러] OG 이미지 크롤링 시작");
        int filled = rssFetchService.fillMissingImageUrlsFromWeb();
        log.info("[RSS 스케줄러] OG 이미지 크롤링 완료 - {}건 채움", filled);
    }
}
