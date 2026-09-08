package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.domain.NewsSource;
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
public class ArticleCategoryAiService {

    // 카테고리 이름을 그대로 답하게 하면 "연예/문화"를 "문화"로 줄여 쓰는 식의 변형이 섞여 파싱이 자주 깨진다.
    // 번호로 답하게 하면 응답에서 숫자 하나만 뽑으면 되고, 목록 밖 값도 범위 검사로 걸러진다.
    private static final String PROMPT_HEADER = """
            다음 뉴스 기사가 어느 분야인지 아래 번호 중 하나로 답해라.
            숫자 하나만 답하고 다른 말은 붙이지 마라.

            """;

    // 분류가 밀려도 이 창을 넘긴 기사는 포기한다. 목록에 오르는 기사는 대부분 오늘·어제 것이라
    // 오래된 기사를 붙들고 있으면 새로 들어온 기사가 계속 뒤로 밀린다.
    private static final int CLASSIFY_WINDOW_DAYS = 3;
    private static final int MAX_BODY_LENGTH = 500;

    private final ArticleRepository articleRepository;
    private final OllamaClient ollamaClient;

    @Value("${app.category.max-per-run}")
    private int maxPerRun;

    public void classifyBatch() {
        LocalDateTime from = LocalDate.now().minusDays(CLASSIFY_WINDOW_DAYS).atStartOfDay();
        List<Article> candidates =
                articleRepository.findUnclassified(from, PageRequest.of(0, maxPerRun));
        if (candidates.isEmpty()) {
            log.info("[카테고리] 분류 대상 없음");
            return;
        }

        List<String> categories = NewsSource.activeCategories();
        int classified = 0;
        int moved = 0;
        int failed = 0;
        for (Article article : candidates) {
            String answer = ollamaClient.generate(prompt(categories, article));
            String category = parseCategory(answer, categories);
            // 파싱 실패한 기사는 그대로 두면 다음 배치에서 다시 후보로 잡힌다.
            if (category == null) {
                failed++;
                continue;
            }
            if (!category.equals(article.getCategory())) moved++;
            apply(article.getId(), category);
            classified++;
        }

        long remaining = articleRepository.countByAiCategoryAtIsNullAndPublishedAtAfter(from);
        log.info("[카테고리] {}건 분류(피드 값과 다른 기사 {}건) · 실패 {}건, 남은 기사 {}건",
                classified, moved, failed, remaining);
    }

    private String prompt(List<String> categories, Article article) {
        StringBuilder sb = new StringBuilder(PROMPT_HEADER);
        for (int i = 0; i < categories.size(); i++) {
            sb.append(i + 1).append(". ").append(categories.get(i)).append('\n');
        }
        sb.append("\n제목: ").append(article.getTitle())
                .append("\n본문: ").append(body(article))
                .append("\n\n번호:");
        return sb.toString();
    }

    // 본문은 중요도 배치가 크롤링해 description에 저장해 둔 것을 쓴다. 여기서 다시 크롤링하지 않는다 —
    // 분야는 앞부분 몇 문장이면 갈리고, 아직 본문이 없는 기사는 제목만으로도 대개 분류된다.
    private String body(Article article) {
        String text = article.getDescription();
        if (text == null) return "";
        String stripped = HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", "")).trim();
        return stripped.length() > MAX_BODY_LENGTH ? stripped.substring(0, MAX_BODY_LENGTH) : stripped;
    }

    private String parseCategory(String answer, List<String> categories) {
        if (answer == null) return null;
        for (int i = 0; i < answer.length(); i++) {
            char c = answer.charAt(i);
            if (c < '0' || c > '9') continue;
            int index = c - '1';
            return index >= 0 && index < categories.size() ? categories.get(index) : null;
        }
        return null;
    }

    // save()가 자체 트랜잭션으로 건별 커밋하므로 여기에 @Transactional을 걸지 않는다.
    private void apply(Long articleId, String category) {
        articleRepository.findById(articleId).ifPresent(article -> {
            article.updateAiCategory(category);
            articleRepository.save(article);
        });
    }
}
