package com.example.neeews.rss.scheduler;

import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssStartupRunner implements ApplicationRunner {

    private final RssFetchService rssFetchService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[RSS 시작] 기존 기사 이미지 URL 보완 시작");
        int updated = rssFetchService.fillMissingImageUrls();
        log.info("[RSS 시작] 이미지 URL 보완 완료 - {}건 처리", updated);
    }
}
