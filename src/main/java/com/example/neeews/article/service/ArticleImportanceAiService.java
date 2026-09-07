package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleImportanceAiService {

    private final ArticleRepository articleRepository;
    private final RssFetchService rssFetchService;
    private final MuniClient muniClient;

    // 인기 목록이 최근 7일치를 보여주므로 판정 대상도 같은 창으로 맞춘다.
    // 이보다 좁으면 목록에 오르는 기사 중 일부가 영구히 미판정으로 남는다.
    private static final int JUDGE_WINDOW_DAYS = 7;

    @Value("${app.importance.max-per-run}")
    private int maxPerRun;

    @Value("${app.importance.batch-size}")
    private int batchSize;

    @Value("${app.importance.threshold}")
    private double threshold;

    public void judgeBatch() {
        LocalDateTime from = LocalDate.now().minusDays(JUDGE_WINDOW_DAYS).atStartOfDay();
        List<Article> candidates =
                articleRepository.findUnjudged(from, PageRequest.of(0, maxPerRun));
        if (candidates.isEmpty()) {
            log.info("[주요기사] 판정 대상 없음");
            return;
        }

        int judged = 0;
        int failed = 0;
        for (int start = 0; start < candidates.size(); start += batchSize) {
            List<Article> chunk = candidates.subList(start, Math.min(start + batchSize, candidates.size()));
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
        log.info("[주요기사] muni {}건 · 실패 {}건 판정, 남은 기사 {}건", judged, failed, remaining);
    }

    // save()가 자체 트랜잭션으로 건별 커밋하므로 여기에 @Transactional을 걸지 않는다.
    // 같은 클래스 내부 호출이라 @Transactional을 걸어도 프록시를 타지 않아 반영되지 않는다.
    private void apply(Long articleId, Importance importance, Double score) {
        articleRepository.findById(articleId).ifPresent(article -> {
            article.updateAiImportance(importance, score);
            articleRepository.save(article);
        });
    }

    // muni는 "제목 줄바꿈 본문" 형태로 학습됐다. 입력 형식을 바꾸면 판정도 같이 흔들린다.
    private String toInput(Article article) {
        return article.getTitle() + "\n" + resolveBody(article);
    }

    // RSS description은 한두 문장뿐이라 muni가 본문으로 받지 않는다. 원문을 크롤링해 길이 제한 없이 통째로 넘긴다.
    private String resolveBody(Article article) {
        String existing = stripHtml(article.getDescription());
        if (article.isContentCrawled()) return existing == null ? "" : existing;

        String crawled = stripHtml(rssFetchService.crawlArticleContent(
                article.getLink(), article.getSource().getDisplayName()));
        if (crawled == null || crawled.isBlank()) return existing == null ? "" : existing;

        // 크롤링 결과를 저장해 두면 요약 배치가 같은 기사를 다시 크롤링하지 않는다.
        articleRepository.findById(article.getId()).ifPresent(found -> {
            found.updateDescription(crawled);
            articleRepository.save(found);
        });
        return crawled;
    }

    private String stripHtml(String text) {
        if (text == null) return null;
        return HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", "")).trim();
    }
}
