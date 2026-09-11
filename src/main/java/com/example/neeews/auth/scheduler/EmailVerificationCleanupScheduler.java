package com.example.neeews.auth.scheduler;

import com.example.neeews.auth.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final EmailVerificationService emailVerificationService;

    // 이미지 정리(3시)와 겹치지 않게 4시에 돌린다.
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpired() {
        try {
            long deleted = emailVerificationService.deleteExpired();
            log.info("[인증 정리] 만료된 이메일 인증 {}건 삭제", deleted);
        } catch (Exception e) {
            log.error("[인증 정리] 배치 실행 실패", e);
        }
    }
}
