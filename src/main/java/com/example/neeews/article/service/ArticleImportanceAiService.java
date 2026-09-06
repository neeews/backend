package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleImportanceAiService {

    // 가이드 1단계 형식 필터. 제목만으로 확정되는 LOW 라 모델을 부르지 않고 걸러낸다.
    private static final Pattern BRACKET_LOW = Pattern.compile(
            "^\\[(게시판|부고|동정|인사|신간|내일날씨|세계의 날씨|헤드라인|건강포커스|imazine|픽!|특징주|마켓뷰|바이오스냅|머니톡스|영상|쇼츠|.*소식)");
    private static final Pattern SERIAL_LOW = Pattern.compile("[①-⑳]");
    private static final Pattern LOCAL_PR_LOW = Pattern.compile(
            "^[가-힣]+(시|군|구|도)(청|의회|교육청)?[ ,]");
    private static final Pattern LOCAL_PR_VERB = Pattern.compile(
            "모집|개최|운영|지원|설치|착공|발행|새단장|선봬|공모전|박람회|체험");
    private static final Pattern MOU_LOW = Pattern.compile("업무협약|MOU|맞손|협약 체결");
    private static final Pattern TARGET_PRICE_LOW = Pattern.compile(
            "^[가-힣A-Za-z]+(증권|투자증권|자산운용)[ ,\"]|목표주가|목표가");

    private static final int MIN_BODY_LENGTH = 20;

    private final ArticleRepository articleRepository;
    private final MuniClient muniClient;

    @Value("${app.importance.max-per-run}")
    private int maxPerRun;

    @Value("${app.importance.max-input-length}")
    private int maxInputLength;

    @Value("${app.importance.batch-size}")
    private int batchSize;

    @Value("${app.importance.threshold}")
    private double threshold;

    public void judgeBatch() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        List<Article> candidates =
                articleRepository.findUnjudged(from, PageRequest.of(0, maxPerRun));
        if (candidates.isEmpty()) {
            log.info("[주요기사] 판정 대상 없음");
            return;
        }

        int filtered = 0;
        List<Article> pending = new ArrayList<>();
        for (Article article : candidates) {
            Importance rule = judgeByRule(article.getTitle());
            if (rule != null) {
                apply(article.getId(), rule, null);
                filtered++;
                continue;
            }
            pending.add(article);
        }

        int judged = 0;
        int failed = 0;
        for (int start = 0; start < pending.size(); start += batchSize) {
            List<Article> chunk = pending.subList(start, Math.min(start + batchSize, pending.size()));
            List<Double> scores = muniClient.score(chunk.stream().map(this::toInput).toList());
            // 판정 못 한 기사는 그대로 두면 다음 배치에서 다시 후보로 잡힌다.
            if (scores == null) {
                failed += chunk.size();
                continue;
            }
            for (int i = 0; i < chunk.size(); i++) {
                double score = scores.get(i);
                apply(chunk.get(i).getId(),
                        score >= threshold ? Importance.HIGH : Importance.LOW, score);
                judged++;
            }
        }

        long remaining = articleRepository.countByAiImportanceIsNullAndPublishedAtAfter(from);
        log.info("[주요기사] 규칙 {}건 · muni {}건 · 실패 {}건 판정, 남은 기사 {}건",
                filtered, judged, failed, remaining);
    }

    // save()가 자체 트랜잭션으로 건별 커밋하므로 여기에 @Transactional을 걸지 않는다.
    // 같은 클래스 내부 호출이라 @Transactional을 걸어도 프록시를 타지 않아 반영되지 않는다.
    private void apply(Long articleId, Importance importance, Double score) {
        articleRepository.findById(articleId).ifPresent(article -> {
            article.updateAiImportance(importance, score);
            articleRepository.save(article);
        });
    }

    private Importance judgeByRule(String title) {
        if (title == null || title.isBlank()) return Importance.LOW;
        String t = title.trim();

        if (BRACKET_LOW.matcher(t).find()) return Importance.LOW;
        if (SERIAL_LOW.matcher(t).find()) return Importance.LOW;
        if (MOU_LOW.matcher(t).find()) return Importance.LOW;
        if (TARGET_PRICE_LOW.matcher(t).find()) return Importance.LOW;
        if (LOCAL_PR_LOW.matcher(t).find() && LOCAL_PR_VERB.matcher(t).find()) return Importance.LOW;

        return null;
    }

    // muni는 "제목 줄바꿈 본문" 형태로 학습됐다. 입력 형식을 바꾸면 판정도 같이 흔들린다.
    private String toInput(Article article) {
        return article.getTitle() + "\n" + truncate(stripHtml(article.getDescription()));
    }

    private String truncate(String body) {
        if (body == null || body.length() < MIN_BODY_LENGTH) return "";
        return body.length() > maxInputLength ? body.substring(0, maxInputLength) : body;
    }

    private String stripHtml(String text) {
        if (text == null) return null;
        return HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", "")).trim();
    }
}
