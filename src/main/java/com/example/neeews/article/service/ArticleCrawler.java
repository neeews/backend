package com.example.neeews.article.service;

import com.example.neeews.rss.service.RssFetchService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// 아직 크롤링 안 된 기사를 처음 열 때 본문·대표 이미지를 가져온다. 외부 사이트를 기다리는 일이라
// 트랜잭션 밖에서 불러야 한다 (안에서 부르면 기다리는 동안 DB 커넥션을 쥐고 있다).
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCrawler {

    // 크롤링은 대부분 외부 응답을 기다리는 I/O라 스레드를 넉넉히 두고, 놀면 반납한다.
    private static final int MAX_CRAWL_THREADS = 64;
    private static final int THREAD_KEEP_ALIVE_SECONDS = 30;

    public record CrawlRequest(Long articleId, String link, String sourceName, String existingImageUrl,
                               boolean needsImage, boolean needsContent) {}

    // 가져오지 못한 항목은 null이다.
    public record CrawlResult(String imageFilename, String content) {}

    private final RssFetchService rssFetchService;
    private final ExternalImageFetcher externalImageFetcher;

    @Value("${app.image.storage-path}")
    private String imageStoragePath;

    private final ThreadPoolExecutor executor = newExecutor();
    private final ConcurrentMap<Long, CompletableFuture<CrawlResult>> inFlight = new ConcurrentHashMap<>();

    // 같은 기사를 동시에 처음 여는 요청이 여러 개여도 크롤링은 한 번만 하고 결과를 나눠 쓴다.
    public CrawlResult crawl(CrawlRequest request) {
        CompletableFuture<CrawlResult> mine = new CompletableFuture<>();
        CompletableFuture<CrawlResult> running = inFlight.putIfAbsent(request.articleId(), mine);
        if (running != null) {
            return running.join();
        }
        try {
            mine.complete(doCrawl(request));
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(request.articleId(), mine);
        }
        return mine.join();
    }

    private CrawlResult doCrawl(CrawlRequest request) {
        CompletableFuture<String> image = request.needsImage()
                ? CompletableFuture.supplyAsync(() -> crawlImage(request), executor)
                : CompletableFuture.completedFuture(null);
        CompletableFuture<String> content = request.needsContent()
                ? CompletableFuture.supplyAsync(() -> rssFetchService.crawlArticleContent(request.link(), request.sourceName()), executor)
                : CompletableFuture.completedFuture(null);
        CompletableFuture.allOf(image, content).join();
        return new CrawlResult(image.join(), content.join());
    }

    private String crawlImage(CrawlRequest request) {
        String url = request.existingImageUrl() != null
                ? request.existingImageUrl()
                : rssFetchService.crawlImageUrl(request.link());
        if (url == null) {
            return null;
        }
        return downloadAndSaveImage(url, request.articleId());
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

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static ThreadPoolExecutor newExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "article-crawl-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(MAX_CRAWL_THREADS, MAX_CRAWL_THREADS,
                THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }
}
