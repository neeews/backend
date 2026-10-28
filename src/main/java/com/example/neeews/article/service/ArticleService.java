package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.domain.Importance;
import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.article.dto.response.ArticleDetailResponse;
import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.dto.response.DailySummaryResponse;
import com.example.neeews.article.dto.response.HeadlineSectionResponse;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.articleread.service.ArticleReadService;
import com.example.neeews.bookmark.service.BookmarkService;
import com.example.neeews.rss.domain.NewsSource;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int HEADLINES_PER_CATEGORY = 5;
    private static final List<String> CATEGORY_ORDER =
            List.of("정치", "경제", "사회", "세계", "IT/과학", "생활/문화", "연예/문화", "스포츠");

    // 같은 사건을 여러 매체가 쓰면 muni 점수도 나란히 높게 나와 상위 칸이 한 사건으로 채워진다.
    // 제목 문자 bigram이 짧은 쪽 기준으로 이 비율 이상 겹치면 같은 사건으로 본다.
    // 오늘 노출된 제목 80건을 쌍으로 재보니 0.45 이상은 전부 같은 사건이었고, 그 아래로는
    // "잠수사 숨져" / "묘지 작업 중 숨져"처럼 표현만 닮은 다른 사건이 섞이기 시작했다.
    private static final double SAME_EVENT_TITLE_OVERLAP = 0.45;

    // 제목만 보면 매체가 같은 사건을 전혀 다른 표현으로 뽑았을 때 놓친다
    // ("법원 '권혁빈 이혼, 2조5500억 재산분할'" / "스마일게이트 권혁빈 이혼 인용...주식 35% 지급" = 제목 0.36).
    // 그래서 본문 겹침을 보조 조건으로 얹는다. 3일치 HIGH 기사 4,468건으로 재보니
    // 본문 0.50 이상은 6쌍 모두 같은 사건이었고, 0.40~0.50 구간은 "여수 거름 중장비 끼임" /
    // "묘지 석축 작업"(0.454)처럼 사고 기사끼리 어휘만 닮은 다른 사건이 섞여 쓸 수 없다.
    private static final double SAME_EVENT_BODY_OVERLAP = 0.50;

    // 요약문은 사건의 핵심만 남아 매체가 달라도 표현이 비슷해진다. 제목만으로는 아슬하게 놓치던
    // 쌍이 요약문에서는 뚜렷하게 갈린다 — "연락사무소 447억 배상" 두 건이 제목 0.41/요약 0.67,
    // "윤석열 위증 2심" 두 건이 제목 0.44/요약 0.57이었다. 반대로 김승원 청문회를 둘러싼 서로 다른
    // 사건들은 0.15~0.26에 머물러, 0.50이면 둘을 가르는 경계로 충분하다.
    private static final double SAME_EVENT_SUMMARY_OVERLAP = 0.50;

    // 본문 전체를 비교하면 정치 공방 기사끼리 같은 배경 설명을 공유해 다른 사건도 0.43까지 올라간다.
    // 사건을 특정하는 정보는 리드 문단에 모여 있어 앞부분만 본다 (위 수치도 이 길이로 측정했다).
    private static final int BODY_PREFIX_LENGTH = 400;
    private static final int HEADLINE_CANDIDATE_LIMIT = HEADLINES_PER_CATEGORY * 4;
    private static final Pattern BRACKET_TAG = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^0-9A-Za-z가-힣]");

    private static final int HOT_TOPIC_WINDOW_HOURS = 48;
    private static final int HOT_FALLBACK_WINDOW_HOURS = 72;
    private static final int HOT_ARTICLE_COUNT = 6;
    // 오늘의 뉴스는 건수를 정해 두지 않는다. 기준을 넘긴 사건은 그날 몇 건이든 다 내보낸다.
    // 다만 자정 직후처럼 오늘 치가 거의 없을 땐 화면이 비어 보여, 이 아래면 어제 치까지 이어 붙인다.
    private static final int TODAY_MIN_COUNT = 5;
    // 한 매체만 쓴 일은 그날의 사건이라기보다 그 매체가 고른 기사에 가깝다.
    private static final int MIN_SOURCES_PER_EVENT = 2;
    // 건수를 고정하진 않지만, 큰 사건이 몰린 날 목록이 끝없이 길어지는 것만 막는다.
    private static final int TODAY_MAX_EVENTS = 20;
    private static final int POPULAR_WINDOW_DAYS = 7;

    // MySQL ngram 파서의 토큰 크기(ngram_token_size 기본값). 이보다 짧은 글자만 있는 검색어는 인덱스로 찾을 수 없다.
    private static final Pattern FULLTEXT_TERM = Pattern.compile("[\\p{L}\\p{N}]{2,}");
    // FULLTEXT 결과를 제목 일치 우선으로 정렬하려면 매칭 행마다 본 테이블을 읽어야 해서 2만 건에 약 3초가 걸린다.
    // 10만 건 기준으로 1,850건은 약 0.2초라, 정렬 의미(제목 우선)를 지키면서 비용이 감당되는 선을 3,000건으로 잡았다.
    private static final long FULLTEXT_SORT_LIMIT = 3_000;
    // IN ()이 빈 목록이 되면 SQL 오류라, 필터가 없거나 일치하는 값이 없을 때 채우는 자리표시 값
    private static final List<String> NO_FILTER_VALUE = List.of("");

    private final ArticleRepository articleRepository;
    private final BookmarkService bookmarkService;
    private final ArticleReadService articleReadService;
    private final RssFetchService rssFetchService;
    private final ExternalImageFetcher externalImageFetcher;
    private final HotTopicService hotTopicService;

    @Value("${app.image.storage-path}")
    private String imageStoragePath;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.summary.min-score}")
    private double summaryMinScore;

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBreakingArticles(String email) {
        return toResponses(articleRepository.findTop10ByOrderByPublishedAtDesc(), email);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getLatestArticles(String email) {
        return toResponses(articleRepository.findTop5ByOrderByPublishedAtDesc(), email);
    }

    @Transactional(readOnly = true)
    public List<HeadlineSectionResponse> getHeadlines(String email) {
        LocalDateTime from = PublishedTime.startOfToday();
        Pageable limit = PageRequest.of(0, HEADLINE_CANDIDATE_LIMIT);

        List<String> categories = new ArrayList<>(articleRepository.findHeadlineCategories(from));
        categories.sort(Comparator.comparingInt(ArticleService::categoryOrder));

        List<HeadlineSectionResponse> sections = new ArrayList<>();
        for (String category : categories) {
            List<Article> articles = distinctEvents(
                    articleRepository.findHeadlines(category, from, limit), HEADLINES_PER_CATEGORY);
            if (articles.isEmpty()) continue;
            sections.add(HeadlineSectionResponse.of(category, toResponses(articles, email)));
        }
        return sections;
    }

    private static int categoryOrder(String category) {
        int index = CATEGORY_ORDER.indexOf(category);
        return index < 0 ? CATEGORY_ORDER.size() : index;
    }

    @Transactional(readOnly = true)
    public List<DailySummaryResponse> getDailySummaries() {
        LocalDateTime todayFrom = PublishedTime.startOfToday();

        List<DailySummaryResponse> events = pickEventsSince(todayFrom);
        if (events.size() < TODAY_MIN_COUNT) {
            events = pickEventsSince(todayFrom.minusDays(1));
        }
        return events;
    }

    // 중요도 점수만으로는 "오늘 있었던 일"을 가릴 수 없다. muni는 뉴스 가치가 있는지를 볼 뿐이라
    // 단발 사건사고도 높게 나온다. 여러 매체가 같은 일을 동시에 다뤘다는 사실이 그보다 나은 신호라,
    // 같은 사건을 버리지 않고 묶어 보도한 매체 수를 센다. 조용한 날은 자연히 적게 나온다.
    private List<DailySummaryResponse> pickEventsSince(LocalDateTime from) {
        return clusterByEvent(articleRepository.findSummarizedImportantSince(from, summaryMinScore)).stream()
                .filter(cluster -> sourceCount(cluster) >= MIN_SOURCES_PER_EVENT)
                .sorted(Comparator.comparingLong(ArticleService::sourceCount).reversed())
                .limit(TODAY_MAX_EVENTS)
                .map(cluster -> DailySummaryResponse.from(cluster.get(0)))
                .toList();
    }

    // 점수 높은 순으로 들어오므로 각 묶음의 첫 기사가 그 사건의 대표가 된다.
    private static List<List<Article>> clusterByEvent(List<Article> candidates) {
        List<List<Article>> clusters = new ArrayList<>();
        for (Article candidate : candidates) {
            clusters.stream()
                    .filter(cluster -> isSameEventAsAny(candidate, cluster))
                    .findFirst()
                    .ifPresentOrElse(
                            cluster -> cluster.add(candidate),
                            () -> clusters.add(new ArrayList<>(List.of(candidate))));
        }
        return clusters;
    }

    // 한 매체가 같은 사건을 여러 번 써도 한 곳으로 센다. 매체 수가 곧 중요도 신호이기 때문이다.
    private static long sourceCount(List<Article> cluster) {
        return cluster.stream().map(a -> a.getSource().getDisplayName()).distinct().count();
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getHotArticles(String email) {
        LocalDateTime fallbackFrom = PublishedTime.now().minusHours(HOT_FALLBACK_WINDOW_HOURS);
        List<Article> articles = pickHotTopicArticles(hotTopicService.getCurrentHotTopics());
        fill(articles, HOT_ARTICLE_COUNT,
                () -> articleRepository.findTop6ByAiImportanceAndPublishedAtAfterOrderByPublishedAtDesc(
                        Importance.HIGH, fallbackFrom));
        // muni 장애로 판정이 밀리면 HIGH만으로는 못 채운다. LOW로 확정된 기사를 올리느니 아직 판정 전인 최신 기사로 남은 칸을 메운다.
        fill(articles, HOT_ARTICLE_COUNT,
                () -> articleRepository.findTop6ByAiImportanceIsNullAndPublishedAtAfterOrderByPublishedAtDesc(
                        fallbackFrom));
        return toResponses(articles, email);
    }

    private void fill(List<Article> target, int limit, Supplier<List<Article>> source) {
        if (target.size() >= limit) return;
        Set<Long> ids = target.stream().map(Article::getId).collect(Collectors.toSet());
        for (Article article : source.get()) {
            if (target.size() >= limit) break;
            if (ids.contains(article.getId()) || isSameEventAsAny(article, target)) continue;
            ids.add(article.getId());
            target.add(article);
        }
    }

    private static List<Article> distinctEvents(List<Article> candidates, int limit) {
        List<Article> picked = new ArrayList<>();
        for (Article candidate : candidates) {
            if (picked.size() >= limit) break;
            if (!isSameEventAsAny(candidate, picked)) picked.add(candidate);
        }
        return picked;
    }

    private static boolean isSameEventAsAny(Article candidate, List<Article> picked) {
        return picked.stream().anyMatch(p -> isSameEvent(candidate, p));
    }

    private static boolean isSameEvent(Article a, Article b) {
        return overlapRatio(charBigrams(a.getTitle()), charBigrams(b.getTitle())) >= SAME_EVENT_TITLE_OVERLAP
                || overlapRatio(summaryBigrams(a), summaryBigrams(b)) >= SAME_EVENT_SUMMARY_OVERLAP
                || overlapRatio(bodyBigrams(a), bodyBigrams(b)) >= SAME_EVENT_BODY_OVERLAP;
    }

    // 요약이 아직 안 달린 기사는 빈 집합이 되어 이 조건으로는 걸리지 않는다 (헤드라인이 그렇다).
    private static Set<String> summaryBigrams(Article article) {
        return charBigrams(article.getAiSummary());
    }

    private static Set<String> bodyBigrams(Article article) {
        String body = article.getDescription();
        if (body == null) return Set.of();
        return charBigrams(body.length() > BODY_PREFIX_LENGTH ? body.substring(0, BODY_PREFIX_LENGTH) : body);
    }

    // 자카드 대신 짧은 쪽 기준 겹침 비율을 쓴다. 같은 사건이라도 매체마다 제목 길이가 두 배씩
    // 차이 나서, 합집합으로 나누면 제목이 긴 쪽 때문에 값이 절반으로 깎인다.
    private static double overlapRatio(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        long common = a.stream().filter(b::contains).count();
        return (double) common / Math.min(a.size(), b.size());
    }

    private static Set<String> charBigrams(String text) {
        if (text == null) return Set.of();
        String cleaned = BRACKET_TAG.matcher(HtmlUtils.htmlUnescape(text)).replaceAll("");
        String normalized = NON_ALPHANUMERIC.matcher(cleaned).replaceAll("").toLowerCase();
        if (normalized.length() < 2) return normalized.isEmpty() ? Set.of() : Set.of(normalized);
        Set<String> bigrams = new HashSet<>();
        for (int i = 0; i < normalized.length() - 1; i++) {
            bigrams.add(normalized.substring(i, i + 2));
        }
        return bigrams;
    }

    // 급상승 주제별 기사 목록을 라운드로빈으로 섞어 여러 이슈가 골고루 노출되게 담는다.
    private List<Article> pickHotTopicArticles(List<String> hotTopics) {
        List<Article> result = new ArrayList<>();
        if (hotTopics.isEmpty()) {
            return result;
        }
        LocalDateTime since = PublishedTime.now().minusHours(HOT_TOPIC_WINDOW_HOURS);
        List<List<Article>> perTopic = hotTopics.stream()
                .map(topic -> articleRepository.findTopByTopicSince(topic, since, PageRequest.of(0, HOT_ARTICLE_COUNT)))
                .toList();
        Set<Long> ids = new HashSet<>();
        int maxSize = perTopic.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < maxSize && result.size() < HOT_ARTICLE_COUNT; i++) {
            for (List<Article> topicArticles : perTopic) {
                if (i >= topicArticles.size() || result.size() >= HOT_ARTICLE_COUNT) continue;
                Article a = topicArticles.get(i);
                if (ids.contains(a.getId()) || isSameEventAsAny(a, result)) continue;
                ids.add(a.getId());
                result.add(a);
            }
        }
        return result;
    }

    private List<ArticleResponse> toResponses(List<Article> articles, String email) {
        Set<Long> readIds = articleReadService.getReadArticleIds(
                email, articles.stream().map(Article::getId).toList());
        return articles.stream()
                .map(a -> ArticleResponse.from(a, readIds.contains(a.getId())))
                .toList();
    }

    @Transactional
    public ArticleDetailResponse getArticleDetail(Long id, String email) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));
        article.incrementViewCount();
        article.updateLastViewedAt(LocalDateTime.now());

        boolean needsImage = article.getCachedImagePath() == null;
        boolean needsUrlFix = !needsImage && !"none".equals(article.getCachedImagePath())
                && (article.getImageUrl() == null || !article.getImageUrl().contains("?w=800"));
        boolean needsContent = !article.isContentCrawled();

        if (needsUrlFix) {
            article.updateCachedImage(baseUrl + "/api/images/" + article.getCachedImagePath() + "?w=800", article.getCachedImagePath());
        }

        if (needsImage || needsContent) {
            String existingImageUrl = article.getImageUrl();
            String articleLink = article.getLink();
            String sourceName = article.getSource().getDisplayName();
            Long articleId = article.getId();

            AtomicReference<String[]> imageResult = new AtomicReference<>();
            AtomicReference<String> contentResult = new AtomicReference<>();

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            if (needsImage) {
                futures.add(CompletableFuture.runAsync(() -> {
                    String url = existingImageUrl != null
                            ? existingImageUrl
                            : rssFetchService.crawlImageUrl(articleLink);
                    if (url == null) return;
                    String filename = downloadAndSaveImage(url, articleId);
                    if (filename != null) imageResult.set(new String[]{url, filename});
                }));
            }

            if (needsContent) {
                futures.add(CompletableFuture.runAsync(() -> {
                    String content = rssFetchService.crawlArticleContent(articleLink, sourceName);
                    if (content != null) contentResult.set(content);
                }));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            String[] imgRes = imageResult.get();
            if (imgRes != null) article.updateCachedImage(baseUrl + "/api/images/" + imgRes[1] + "?w=800", imgRes[1]);
            else if (needsImage) article.updateCachedImage(null, "none"); // 이미지 없음으로 확정, 재시도 방지

            String content = contentResult.get();
            if (content != null) article.updateDescription(content);
            else if (needsContent) article.markContentCrawled();
        }

        articleReadService.markAsRead(id, email);

        List<ArticleResponse> related = getRelated(article);
        boolean isBookmarked = bookmarkService.isBookmarked(id, email);
        return ArticleDetailResponse.of(article, related, isBookmarked, email != null);
    }

    private String downloadAndSaveImage(String url, Long articleId) {
        ExternalImageFetcher.FetchedImage image = externalImageFetcher.fetch(url);
        if (image == null) {
            return null;
        }
        try {
            String filename = articleId + "." + image.ext();
            Path path = Paths.get(imageStoragePath, filename);
            Files.createDirectories(path.getParent());
            Files.write(path, image.bytes());
            return filename;
        } catch (Exception e) {
            log.warn("[이미지 캐시] 저장 실패 articleId={}: {}", articleId, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticlesByCategory(String category, String sort, int page, String email) {
        LocalDateTime now = PublishedTime.now();
        Page<Article> result = "popular".equals(sort)
                ? articleRepository.findByCategoryOrderByPopularity(category,
                        now.minusDays(POPULAR_WINDOW_DAYS), now, PageRequest.of(page - 1, 21))
                : articleRepository.findByCategoryOptional(category,
                        PageRequest.of(page - 1, 21, Sort.by(Sort.Direction.DESC, "publishedAt")));
        return toResponsePage(result, email);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> searchArticles(String q, List<String> categories, List<String> sourceNames, Pageable pageable, String email) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        boolean hasCategories = categories != null && !categories.isEmpty();
        boolean hasSources = sourceNames != null && !sourceNames.isEmpty();

        if (FULLTEXT_TERM.matcher(q).find()) {
            return toResponsePage(searchByFulltext(q, hasCategories ? categories : List.of(), sourceNames, unsorted), email);
        }

        if (hasCategories && hasSources) {
            List<NewsSource> sources = resolveNewsSources(sourceNames);
            return toResponsePage(articleRepository.searchByKeywordAndSourcesAndCategories(q, sources, categories, unsorted), email);
        }
        if (hasCategories) {
            return toResponsePage(articleRepository.searchByKeywordAndCategories(q, categories, unsorted), email);
        }
        if (hasSources) {
            List<NewsSource> sources = resolveNewsSources(sourceNames);
            return toResponsePage(articleRepository.searchByKeywordAndSources(q, sources, unsorted), email);
        }
        return toResponsePage(articleRepository.searchByKeyword(q, unsorted), email);
    }

    // ngram은 검색어를 2글자 단위로 쪼개 따옴표 구문으로 찾는다. 따옴표로 감싸야 띄어쓰기까지 포함한 부분 문자열 검색(LIKE와 같은 의미)이 된다.
    private Page<Article> searchByFulltext(String q, List<String> categories, List<String> sourceNames, Pageable pageable) {
        boolean anySource = sourceNames == null || sourceNames.isEmpty();
        List<NewsSource> sources = anySource ? List.of() : resolveNewsSources(sourceNames);
        if (!anySource && sources.isEmpty()) return Page.empty(pageable);

        String phrase = "\"" + q.replace('"', ' ').strip() + "\"";
        long matches = articleRepository.countFulltext(phrase);
        if (matches == 0) return Page.empty(pageable);

        boolean anyCategory = categories.isEmpty();
        List<String> categoryFilter = anyCategory ? NO_FILTER_VALUE : categories;
        long total = anyCategory && anySource ? matches : articleRepository.countFulltext(
                phrase, anyCategory, categoryFilter, anySource, anySource ? NO_FILTER_VALUE : sourceNamesOf(sources));

        List<Article> content = matches > FULLTEXT_SORT_LIMIT
                ? articleRepository.searchNewestFirst(q, anyCategory, categoryFilter, anySource,
                        anySource ? List.of(NewsSource.values()[0]) : sources, pageable)
                : articleRepository.searchFulltext(phrase, q, anyCategory, categoryFilter, anySource,
                        anySource ? NO_FILTER_VALUE : sourceNamesOf(sources), pageable);
        return new PageImpl<>(content, pageable, total);
    }

    private List<String> sourceNamesOf(List<NewsSource> sources) {
        return sources.stream().map(NewsSource::name).toList();
    }

    private Page<ArticleResponse> toResponsePage(Page<Article> page, String email) {
        Set<Long> readIds = articleReadService.getReadArticleIds(
                email, page.getContent().stream().map(Article::getId).toList());
        return page.map(a -> ArticleResponse.from(a, readIds.contains(a.getId())));
    }

    private List<NewsSource> resolveNewsSources(List<String> displayNames) {
        return Arrays.stream(NewsSource.values())
                .filter(s -> displayNames.contains(s.getDisplayName()))
                .toList();
    }

    private List<ArticleResponse> getRelated(Article article) {
        List<Article> related = null;
        if (article.getCategory() != null) {
            related = articleRepository.findTop5ByCategoryAndIdNotOrderByPublishedAtDesc(
                    article.getCategory(), article.getId());
        }
        if (related == null || related.isEmpty()) {
            Pageable p = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "publishedAt"));
            related = articleRepository.findBySource(article.getSource(), p)
                    .stream().filter(a -> !a.getId().equals(article.getId())).limit(5).toList();
        }
        return related.stream().map(ArticleResponse::from).toList();
    }
}
