package com.example.neeews.briefing.scheduler;

import com.example.neeews.briefing.service.BriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 실제 사용자에게 메일이 나가는 작업이라 기본값을 꺼 둔다(app.briefing.enabled). 로컬에서 운영 DB에 붙여 띄웠을 때
// 개발 PC가 구독자 전원에게 메일을 보내는 일을 막는다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.briefing.enabled", havingValue = "true")
public class BriefingScheduler {

    private static final String SEOUL = "Asia/Seoul";

    private final BriefingService briefingService;

    // 서버는 UTC로 돈다. 시간대를 지정하지 않으면 한국 시간 오후 4시에 나간다.
    @Scheduled(cron = "0 0 7 * * *", zone = SEOUL)
    public void sendDailyBriefing() {
        try {
            int sent = briefingService.sendDailyBriefing();
            log.info("[브리핑] {}명에게 발송", sent);
        } catch (Exception e) {
            log.error("[브리핑] 발송 실패", e);
        }
    }
}
