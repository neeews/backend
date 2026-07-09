package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.dto.response.ArticleDetailResponse;
import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.articleread.service.ArticleReadService;
import com.example.neeews.bookmark.service.BookmarkService;
import com.example.neeews.rss.domain.NewsSource;
import com.example.neeews.rss.service.RssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int HOT_TOPIC_WINDOW_HOURS = 48;
    private static final int HOT_FALLBACK_WINDOW_HOURS = 72;

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

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBreakingArticles(String email) {
        return toResponses(articleRepository.findTop10ByOrderByPublishedAtDesc(), email);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getLatestArticles(String email) {
        return toResponses(articleRepository.findTop5ByOrderByPublishedAtDesc(), email);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getHotArticles(String email) {
        String hotTopic = hotTopicService.getCurrentHotTopic();
        List<Article> articles = hotTopic != null
                ? articleRepository.findTopByTopicSince(hotTopic,
                        LocalDateTime.now().minusHours(HOT_TOPIC_WINDOW_HOURS), PageRequest.of(0, 6))
                : List.of();
        if (articles.isEmpty()) {
            articles = articleRepository.findTop6ByPublishedAtAfterOrderByViewCountDesc(
                    LocalDateTime.now().minusHours(HOT_FALLBACK_WINDOW_HOURS));
        }
        return toResponses(articles, email);
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
        Sort s = "popular".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "viewCount")
                : Sort.by(Sort.Direction.DESC, "publishedAt");
        Pageable pageable = PageRequest.of(page - 1, 21, s);
        return toResponsePage(articleRepository.findByCategoryOptional(category, pageable), email);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> searchArticles(String q, List<String> categories, List<String> sourceNames, Pageable pageable, String email) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        boolean hasCategories = categories != null && !categories.isEmpty();
        boolean hasSources = sourceNames != null && !sourceNames.isEmpty();

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
