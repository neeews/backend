package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.domain.NewsSource;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSummaryService {

    private static final String PROMPT = """
            다음 뉴스 기사를 한국어 3문장으로 요약해라. \
            군더더기 없이 핵심 사실만 담고, 요약문 외에 다른 말은 붙이지 마라.

            기사:
            """;

    // 요약할 만한 최소 분량. RSS description만 있는 기사는 평균 79자라 이 아래는 요약해도 원문보다 길어진다.
    private static final int MIN_BODY_LENGTH = 300;
    private static final int CANDIDATE_WINDOW_HOURS = 12;

    private final ArticleRepository articleRepository;
    private final RssFetchService rssFetchService;
    private final OllamaClient ollamaClient;

    @Value("${app.summary.batch-size}")
    private int batchSize;

    @Value("${app.summary.max-input-length}")
    private int maxInputLength;

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

    // 카테고리마다 최신 미요약 기사를 1건씩 뽑되, 마지막으로 요약된 지 오래된 카테고리를 우선한다.
    // 카테고리(7개)가 배치 크기(5건)보다 많아 매 실행 같은 카테고리만 뽑히는 것을 막는다.
    private List<Article> pickCandidates() {
        LocalDateTime since = LocalDateTime.now().minusHours(CANDIDATE_WINDOW_HOURS);
        Map<String, LocalDateTime> lastSummarized = lastSummarizedAtByCategory();

        List<String> categories = new ArrayList<>(activeCategories());
        categories.sort(Comparator.comparing(
                c -> lastSummarized.getOrDefault(c, LocalDateTime.MIN)));

        List<Article> picked = new ArrayList<>();
        for (String category : categories) {
            if (picked.size() >= batchSize) break;
            Optional<Article> candidate = articleRepository
                    .findTop1ByCategoryAndAiSummaryIsNullAndPublishedAtAfterOrderByPublishedAtDesc(category, since);
            candidate.ifPresent(picked::add);
        }
        return picked;
    }

    private List<String> activeCategories() {
        return java.util.Arrays.stream(NewsSource.values())
                .map(NewsSource::getCategory)
                .filter(c -> !"종합".equals(c))
                .distinct()
                .toList();
    }

    private Map<String, LocalDateTime> lastSummarizedAtByCategory() {
        Map<String, LocalDateTime> map = new HashMap<>();
        for (Object[] row : articleRepository.findLastSummarizedAtByCategory()) {
            if (row[0] != null && row[1] != null) {
                map.put((String) row[0], (LocalDateTime) row[1]);
            }
        }
        return map;
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
