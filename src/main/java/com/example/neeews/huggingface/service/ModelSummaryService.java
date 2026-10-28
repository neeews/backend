package com.example.neeews.huggingface.service;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.article.service.OllamaClient;
import com.example.neeews.huggingface.domain.ModelRelease;
import com.example.neeews.huggingface.repository.ModelReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSummaryService {

    // 숫자를 빼게 하는 이유: 작은 모델은 "209M"을 "209만", "120B"를 "120억"으로 옮기는 식으로 단위를 틀린다.
    // 파라미터 수는 HF 값을 응답에 따로 싣는다.
    private static final String PROMPT = """
            다음은 Hugging Face에 공개된 AI 모델의 소개 문서다. \
            이 모델이 무엇이고 무엇을 할 수 있는지 한국어 두 문장으로 설명해라. \
            모델 이름은 원문 그대로 쓰고, 숫자(파라미터 수·벤치마크 점수·문맥 길이)와 설치·사용 방법은 빼라. \
            "다음은" 같은 머리말 없이 설명문만 써라.

            문서:
            """;

    // 지시를 어기고 "Here is a two-sentence summary ...:" 같은 머리말 줄을 먼저 붙이는 경우가 많다(llama3로 7건 중 6건).
    private static final Pattern PREAMBLE_LINE = Pattern.compile("\\A[^\\n]*[:：]\\s*\\n+");
    // 한글이 한 글자도 없으면 지시를 무시하고 영어로 답한 것이다.
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    // 수집 창과 같다. 이보다 오래된 모델은 카드를 못 읽거나 요약이 계속 실패해도 더 붙잡지 않는다.
    private static final int SUMMARY_WINDOW_DAYS = 30;
    private static final int MAX_PER_RUN = 10;
    // 소개는 카드 앞쪽에 있고 뒤쪽은 사용법과 평가 결과다. 앞부분만 넘겨야 작은 모델이 소개에 집중한다.
    private static final int MAX_INPUT_LENGTH = 2000;
    // 이보다 짧으면 링크·배지뿐인 카드라 설명할 거리가 없다.
    private static final int MIN_INPUT_LENGTH = 200;

    private final HuggingFaceClient huggingFaceClient;
    private final OllamaClient ollamaClient;
    private final ModelReleaseRepository modelReleaseRepository;

    // 노출 쿼리와 같은 기준을 써야 한다. 수집한 모델 대부분은 기준을 못 넘는 부산물이라 요약해도 화면에 안 뜬다.
    @Value("${app.huggingface.release-min-likes}")
    private long minLikes;

    public void summarizeBatch() {
        List<ModelRelease> candidates = modelReleaseRepository.findByLikesGreaterThanEqualAndAiSummaryIsNullAndReleasedAtAfter(
                minLikes, PublishedTime.now().minusDays(SUMMARY_WINDOW_DAYS),
                PageRequest.of(0, MAX_PER_RUN, Sort.by(Sort.Direction.DESC, "likes")));
        if (candidates.isEmpty()) {
            log.info("[모델 요약] 대상 모델 없음");
            return;
        }

        int done = 0;
        for (ModelRelease release : candidates) {
            String card = huggingFaceClient.findModelCardText(release.getModelId());
            if (card == null || card.length() < MIN_INPUT_LENGTH) {
                log.warn("[모델 요약] 모델 카드 확보 실패 model={}", release.getModelId());
                continue;
            }

            String output = ollamaClient.generate(PROMPT + truncate(card));
            if (output == null) continue;

            String summary = clean(output);
            if (summary == null) {
                log.warn("[모델 요약] 한국어 요약이 아님 model={} output={}", release.getModelId(), output);
                continue;
            }

            applySummary(release.getId(), summary);
            done++;
        }
        log.info("[모델 요약] {}건 중 {}건 완료", candidates.size(), done);
    }

    private String clean(String output) {
        String text = PREAMBLE_LINE.matcher(output).replaceFirst("").trim();
        return HANGUL.matcher(text).find() ? text : null;
    }

    private String truncate(String text) {
        return text.length() > MAX_INPUT_LENGTH ? text.substring(0, MAX_INPUT_LENGTH) : text;
    }

    // 요약 1건에 수 초씩 걸려 배치 전체를 한 트랜잭션으로 묶지 않는다. save()가 자체 트랜잭션으로 건별 커밋한다.
    private void applySummary(Long releaseId, String summary) {
        modelReleaseRepository.findById(releaseId).ifPresent(release -> {
            release.updateAiSummary(summary);
            modelReleaseRepository.save(release);
        });
    }
}
