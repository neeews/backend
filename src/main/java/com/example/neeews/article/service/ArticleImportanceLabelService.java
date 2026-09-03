package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.ArticleImportanceLabel;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.domain.LabelOrigin;
import com.example.neeews.article.dto.request.SeedFilter;
import com.example.neeews.article.dto.response.ImportanceLabelResponse;
import com.example.neeews.article.dto.response.LabelingArticleResponse;
import com.example.neeews.article.repository.ArticleImportanceLabelRepository;
import com.example.neeews.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleImportanceLabelService {

    // 라벨을 매길 때 적용한 기준 문서 판본. LABELING_GUIDE.md 를 고치면 여기도 올린다.
    private static final String GUIDE_VERSION = "2026-09-03";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int SELF_AGREEMENT_ROUND = 1;

    private final ArticleRepository articleRepository;
    private final ArticleImportanceLabelRepository labelRepository;

    @Transactional(readOnly = true)
    public List<LabelingArticleResponse> findNext(String labeledBy, int round, int size, SeedFilter filter) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("한 번에 가져올 기사 수는 1~" + MAX_PAGE_SIZE + "건입니다.");
        }
        PageRequest page = PageRequest.of(0, size);
        List<Article> articles = switch (filter) {
            case SEED_ONLY -> articleRepository.findUnlabeledByWithAiLabel(labeledBy, round, page);
            case NO_SEED -> articleRepository.findUnlabeledByWithoutAiLabel(labeledBy, round, page);
            case ALL -> articleRepository.findUnlabeledBy(labeledBy, round, page);
        };
        return articles.stream().map(LabelingArticleResponse::from).toList();
    }

    // 이 경로로 들어오는 라벨은 전부 사람이 매긴 것이다. origin 을 요청에서 받지 않고 서버가 박는다.
    @Transactional
    public ImportanceLabelResponse label(Long articleId, Importance label, String labeledBy, int round) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));

        ArticleImportanceLabel saved = labelRepository
                .findByArticle_IdAndLabeledByAndRound(articleId, labeledBy, round)
                .map(existing -> {
                    existing.updateLabel(label);
                    return existing;
                })
                .orElseGet(() -> labelRepository.save(ArticleImportanceLabel.builder()
                        .article(article)
                        .label(label)
                        .labeledBy(labeledBy)
                        .origin(LabelOrigin.HUMAN)
                        .round(round)
                        .guideVersion(GUIDE_VERSION)
                        .build()));

        log.info("중요도 라벨 저장: article={} label={} by={} round={}", articleId, label, labeledBy, round);
        return ImportanceLabelResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(String labeledBy) {
        List<ArticleImportanceLabel> first = labelRepository.findByLabeledByAndRound(labeledBy, 0);
        List<ArticleImportanceLabel> second = labelRepository.findByLabeledByAndRound(labeledBy, SELF_AGREEMENT_ROUND);

        Map<String, Long> distribution = new HashMap<>();
        for (Object[] row : labelRepository.findLabelDistributionBy(labeledBy, 0)) {
            distribution.put(((Importance) row[0]).name(), (Long) row[1]);
        }

        Map<Long, Importance> firstByArticle = first.stream()
                .collect(Collectors.toMap(l -> l.getArticle().getId(), ArticleImportanceLabel::getLabel));

        return Map.of(
                "labeledBy", labeledBy,
                "total", first.size(),
                "distribution", distribution,
                "guideVersion", GUIDE_VERSION,
                "seedAgreement", agreementWithAi(firstByArticle),
                "selfAgreement", selfAgreement(firstByArticle, second)
        );
    }

    // 내 라벨과 AI 시드 라벨의 일치율 — 시드를 학습 데이터로 믿어도 되는지의 근거
    private Map<String, Object> agreementWithAi(Map<Long, Importance> mine) {
        if (mine.isEmpty()) {
            return Map.of("compared", 0);
        }
        List<ArticleImportanceLabel> aiLabels =
                labelRepository.findByOriginAndArticle_IdIn(LabelOrigin.AI, mine.keySet());
        long matched = aiLabels.stream()
                .filter(ai -> ai.getLabel() == mine.get(ai.getArticle().getId()))
                .count();
        return rate(aiLabels.size(), matched);
    }

    // 같은 기사를 다시 매겼을 때의 일치율 — 모델에게 요구할 수 있는 성능의 상한선
    private Map<String, Object> selfAgreement(Map<Long, Importance> first, List<ArticleImportanceLabel> second) {
        Set<Long> firstIds = first.keySet();
        List<ArticleImportanceLabel> overlap = second.stream()
                .filter(l -> firstIds.contains(l.getArticle().getId()))
                .toList();
        long matched = overlap.stream()
                .filter(l -> l.getLabel() == first.get(l.getArticle().getId()))
                .count();
        return rate(overlap.size(), matched);
    }

    private Map<String, Object> rate(int compared, long matched) {
        if (compared == 0) {
            return Map.of("compared", 0);
        }
        return Map.of(
                "compared", compared,
                "matched", matched,
                "rate", Math.round((double) matched / compared * 1000) / 10.0
        );
    }
}
