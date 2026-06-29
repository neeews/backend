package com.example.neeews.article.scheduler;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ArticleRepository articleRepository;

    @Value("${app.image.storage-path}")
    private String imageStoragePath;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<Article> expired = articleRepository.findExpiredCachedImages(threshold);
        int deleted = 0;
        for (Article article : expired) {
            String filename = article.getCachedImagePath();
            try {
                Files.deleteIfExists(Paths.get(imageStoragePath, filename));
                deleteCachedResizes(filename);
                deleted++;
            } catch (Exception e) {
                log.warn("[이미지 정리] 파일 삭제 실패: {}", filename);
            }
            article.clearCachedImage();
        }
        log.info("[이미지 정리] 7일 이상 미조회 기사 이미지 {}건 삭제", deleted);
    }

    private void deleteCachedResizes(String filename) throws IOException {
        Path cacheRoot = Paths.get(imageStoragePath, "cache");
        if (!Files.isDirectory(cacheRoot)) return;
        try (var widthDirs = Files.list(cacheRoot)) {
            widthDirs.filter(Files::isDirectory).forEach(dir -> {
                try {
                    Files.deleteIfExists(dir.resolve(filename));
                } catch (IOException ignored) {}
            });
        }
    }
}
