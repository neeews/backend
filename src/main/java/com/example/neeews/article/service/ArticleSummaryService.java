package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSummaryService {

    private static final String PROMPT = """
            다음 뉴스 기사를 한국어 한두 문장으로 요약해라. \
            무슨 일이 있었는지만 담고, 요약문 외에 다른 말은 붙이지 마라.

            기사:
            """;

    // 요약할 만한 최소 분량. RSS description만 있는 기사는 평균 79자라 이 아래는 요약해도 원문보다 길어진다.
    private static final int MIN_BODY_LENGTH = 300;
    // 오늘의 뉴스가 자정 직후 비지 않도록 어제 기사까지 요약해 둔다 (getDailySummaries의 어제 보충과 같은 창).
    private static final int CANDIDATE_WINDOW_HOURS = 24;

    private final ArticleRepository articleRepository;
    private final RssFetchService rssFetchService;
    private final OllamaClient ollamaClient;

    @Value("${app.summary.batch-size}")
    private int batchSize;

    @Value("${app.summary.max-input-length}")
    private int maxInputLength;

    @Value("${app.summary.min-score}")
    private double minScore;

    public void summarizeBatch() {
        List<Article> candidates = pickCandidates();
        if (candidates.isEmpty()) {
            log.info("[요약] 대상 기사 없음");
            return;
        }

        int done = 0;
        for (Article article : candidates) {
            String body = resolveBody(article);
            if (body == null) {
                log.warn("[요약] 본문 확보 실패 id={} source={}", article.getId(), article.getSource());
                continue;
            }

            String summary = ollamaClient.generate(PROMPT + truncate(body));
            if (summary == null) continue;

            applySummary(article.getId(), body, summary);
            done++;
        }
        log.info("[요약] {}건 중 {}건 완료", candidates.size(), done);
    }

    // 오늘의 뉴스에 올라갈 기사만 요약한다. 카테고리 골고루 대신 중요도 점수 높은 순으로 뽑는 이유는,
    // 화면이 "오늘 있었던 중요한 일"만 담는 곳이라 카테고리가 비어도 상관없기 때문이다.
    // 노출 쿼리와 같은 min-score를 써야, 요약해 놓고 목록에 안 뜨는 기사가 생기지 않는다.
    private List<Article> pickCandidates() {
        LocalDateTime since = LocalDateTime.now().minusHours(CANDIDATE_WINDOW_HOURS);
        return articleRepository.findUnsummarizedImportant(since, minScore, PageRequest.of(0, batchSize));
    }

    private String resolveBody(Article article) {
        String existing = stripHtml(article.getDescription());
        if (article.isContentCrawled() && isLongEnough(existing)) return existing;

        String crawled = stripHtml(
                rssFetchService.crawlArticleContent(article.getLink(), article.getSource().getDisplayName()));
        if (isLongEnough(crawled)) return crawled;

        return isLongEnough(existing) ? existing : null;
    }

    private boolean isLongEnough(String text) {
        return text != null && text.length() >= MIN_BODY_LENGTH;
    }

    private String stripHtml(String text) {
        if (text == null) return null;
        return HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", "")).trim();
    }

    private String truncate(String body) {
        return body.length() > maxInputLength ? body.substring(0, maxInputLength) : body;
    }

    // 요약 1건에 10초 이상 걸려 배치 전체를 한 트랜잭션으로 묶지 않는다.
    // save()가 자체 트랜잭션으로 건별 커밋하므로 여기에 @Transactional을 걸지 않는다.
    private void applySummary(Long articleId, String body, String summary) {
        articleRepository.findById(articleId).ifPresent(article -> {
            if (!article.isContentCrawled()) article.updateDescription(body);
            article.updateAiSummary(summary);
            articleRepository.save(article);
        });
    }
}
