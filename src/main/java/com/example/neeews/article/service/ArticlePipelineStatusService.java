package com.example.neeews.article.service;

import com.example.neeews.article.dto.response.PipelineStatusResponse;
import com.example.neeews.article.dto.response.PipelineStatusResponse.Status;
import com.example.neeews.article.dto.response.PipelineStatusResponse.StageStatus;
import com.example.neeews.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// muni·Ollama를 직접 찌르지 않고 DB에 쌓인 흔적으로 판정한다. 헬스체크는 자주 호출되는데
// 매번 추론을 돌리면 그 자체가 부하가 되고, 실제로 문제가 됐던 건 "외부가 죽는" 경우보다
// "배치가 돌긴 도는데 결과가 안 쌓이는" 조용한 정체였다.
@Service
@RequiredArgsConstructor
public class ArticlePipelineStatusService {

    // RSS는 10분 주기다. 두 번 걸렀으면 이상, 여섯 번이면 멈춘 것으로 본다.
    private static final int RSS_WARN_MINUTES = 30;
    private static final int RSS_DOWN_MINUTES = 60;

    // 판정은 30분 주기에 회당 400건이라, 이보다 밀리면 한 주기로 못 따라잡는다.
    private static final long IMPORTANCE_WARN_BACKLOG = 400;
    private static final int IMPORTANCE_JUDGE_WINDOW_DAYS = 7;

    // 요약은 매시간 batch-size만큼만 처리한다. 하루치를 넘겨 밀리면 계속 뒤처진다.
    private static final long SUMMARY_WARN_BACKLOG = 200;
    private static final int SUMMARY_WARN_HOURS = 3;
    private static final int SUMMARY_DOWN_HOURS = 12;
    private static final int SUMMARY_WINDOW_HOURS = 24;

    private final ArticleRepository articleRepository;

    @Value("${app.summary.min-score}")
    private double summaryMinScore;

    @Transactional(readOnly = true)
    public PipelineStatusResponse getStatus() {
        return PipelineStatusResponse.of(List.of(rssStage(), importanceStage(), summaryStage()));
    }

    private StageStatus rssStage() {
        LocalDateTime lastFetched = articleRepository.findLastFetchedAt();
        if (lastFetched == null) {
            return StageStatus.of("rss", Status.DOWN, "수집된 기사가 없습니다.", null, null);
        }

        long idleMinutes = minutesSince(lastFetched);
        long recent = articleRepository.countByFetchedAtAfter(LocalDateTime.now().minusHours(1));

        if (idleMinutes >= RSS_DOWN_MINUTES) {
            return StageStatus.of("rss", Status.DOWN,
                    idleMinutes + "분째 새 기사가 들어오지 않았습니다.", lastFetched, null);
        }
        if (idleMinutes >= RSS_WARN_MINUTES) {
            return StageStatus.of("rss", Status.WARN,
                    idleMinutes + "분째 수집이 없습니다.", lastFetched, null);
        }
        return StageStatus.of("rss", Status.UP,
                "최근 1시간 " + recent + "건 수집", lastFetched, null);
    }

    private StageStatus importanceStage() {
        LocalDateTime from = LocalDate.now().minusDays(IMPORTANCE_JUDGE_WINDOW_DAYS).atStartOfDay();
        long backlog = articleRepository.countByAiImportanceIsNullAndPublishedAtAfter(from);
        LocalDateTime lastJudged = articleRepository.findLastImportanceJudgedAt();

        if (lastJudged == null) {
            return StageStatus.of("importance", Status.DOWN,
                    "판정된 기사가 없습니다. muni 연결을 확인해주세요.", null, backlog);
        }
        if (backlog >= IMPORTANCE_WARN_BACKLOG) {
            return StageStatus.of("importance", Status.WARN,
                    "미판정 " + backlog + "건이 밀려 있습니다.", lastJudged, backlog);
        }
        return StageStatus.of("importance", Status.UP,
                "미판정 " + backlog + "건", lastJudged, backlog);
    }

    private StageStatus summaryStage() {
        LocalDateTime windowFrom = LocalDateTime.now().minusHours(SUMMARY_WINDOW_HOURS);
        long backlog = articleRepository.countSummaryBacklog(
                windowFrom, summaryMinScore, ArticleSummaryService.MIN_BODY_LENGTH);
        long todayCount = articleRepository.countSummarizedImportantSince(
                LocalDate.now().atStartOfDay(), summaryMinScore);
        LocalDateTime lastSummarized = articleRepository.findLastSummarizedAt();

        if (lastSummarized == null) {
            return StageStatus.of("summary", Status.DOWN,
                    "요약된 기사가 없습니다. Ollama 연결을 확인해주세요.", null, backlog);
        }

        long idleHours = hoursSince(lastSummarized);
        // 밀린 게 없으면 요약이 뜸한 건 정상이다. 할 일이 남았는데도 멈춰 있을 때만 문제로 본다.
        if (backlog > 0 && idleHours >= SUMMARY_DOWN_HOURS) {
            return StageStatus.of("summary", Status.DOWN,
                    idleHours + "시간째 요약이 없는데 " + backlog + "건이 대기 중입니다.",
                    lastSummarized, backlog);
        }
        if (backlog > 0 && idleHours >= SUMMARY_WARN_HOURS) {
            return StageStatus.of("summary", Status.WARN,
                    idleHours + "시간째 요약이 없습니다.", lastSummarized, backlog);
        }
        if (backlog >= SUMMARY_WARN_BACKLOG) {
            return StageStatus.of("summary", Status.WARN,
                    "미요약 " + backlog + "건이 밀려 있습니다.", lastSummarized, backlog);
        }
        return StageStatus.of("summary", Status.UP,
                "오늘 " + todayCount + "건 요약 · 대기 " + backlog + "건", lastSummarized, backlog);
    }

    private static long minutesSince(LocalDateTime at) {
        return Duration.between(at, LocalDateTime.now()).toMinutes();
    }

    private static long hoursSince(LocalDateTime at) {
        return Duration.between(at, LocalDateTime.now()).toHours();
    }
}
