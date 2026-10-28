package com.example.neeews.huggingface.scheduler;

import com.example.neeews.huggingface.service.ModelSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 기사 요약과 같은 Ollama를 쓰므로 같은 스위치(app.summary.enabled)로 함께 켜고 끈다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.summary.enabled", havingValue = "true")
public class ModelSummaryScheduler {

    private final ModelSummaryService modelSummaryService;

    // 기사 요약(매시 10분)과 겹치지 않는 시각에 돈다.
    @Scheduled(cron = "0 40 * * * *")
    public void summarize() {
        try {
            modelSummaryService.summarizeBatch();
        } catch (Exception e) {
            log.error("[모델 요약] 배치 실행 실패", e);
        }
    }
}
