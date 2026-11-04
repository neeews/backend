package com.example.neeews.devstack.scheduler;

import com.example.neeews.devstack.service.StackUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StackUpdateScheduler {

    // 한 번에 (저장소 수 + 1)번 부른다. 토큰 없는 한도(시간당 60회) 안에서 저장소를 50개까지 둘 수 있다.
    private static final long REFRESH_INTERVAL_MINUTES = 60;

    private final StackUpdateService stackUpdateService;

    // 메모리에만 두는 목록이라 기동 직후 비어 있다. fixedDelay라 기동하자마자 한 번 채운다.
    @Scheduled(fixedDelay = REFRESH_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        try {
            stackUpdateService.refresh();
        } catch (Exception e) {
            log.error("[개발 스택] 갱신 실패", e);
        }
    }
}
