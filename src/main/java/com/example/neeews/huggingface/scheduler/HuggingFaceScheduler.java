package com.example.neeews.huggingface.scheduler;

import com.example.neeews.huggingface.service.ModelReleaseService;
import com.example.neeews.huggingface.service.PaperService;
import com.example.neeews.huggingface.service.TrendingModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HuggingFaceScheduler {

    private static final long RELEASE_INTERVAL_MINUTES = 60;
    // 트렌딩 점수와 논문 추천 수는 시간 단위로 바뀐다. 이보다 자주 받아도 순위가 거의 그대로다.
    private static final long REFRESH_INTERVAL_MINUTES = 30;

    private final ModelReleaseService modelReleaseService;
    private final TrendingModelService trendingModelService;
    private final PaperService paperService;

    @Scheduled(fixedDelay = RELEASE_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void collectReleases() {
        try {
            int added = modelReleaseService.collect();
            log.info("[HuggingFace] 새 모델 {}건 저장", added);
        } catch (Exception e) {
            log.error("[HuggingFace] 모델 수집 실패", e);
        }
    }

    // 메모리에만 두는 목록이라 기동 직후 비어 있다. fixedDelay라 기동하자마자 한 번 채운다.
    @Scheduled(fixedDelay = REFRESH_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void refreshTrending() {
        try {
            trendingModelService.refresh();
        } catch (Exception e) {
            log.error("[HuggingFace] 트렌딩 갱신 실패", e);
        }
    }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void refreshDailyPapers() {
        try {
            paperService.refresh();
        } catch (Exception e) {
            log.error("[HuggingFace] 논문 갱신 실패", e);
        }
    }
}
