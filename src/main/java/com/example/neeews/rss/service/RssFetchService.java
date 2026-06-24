package com.example.neeews.rss.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.domain.NewsSource;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFetchService {

    private final ArticleRepository articleRepository;

    @Transactional
    public int fetchAll() {
        return Arrays.stream(NewsSource.values())
                .mapToInt(this::fetchSource)
                .sum();
    }

    @Transactional
    public int fetchSource(NewsSource source) {
        int saved = 0;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(source.getRssUrl()).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; NeeewsBot/1.0)");
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            SyndFeed feed;
            try (InputStream is = conn.getInputStream()) {
                feed = new SyndFeedInput().build(new XmlReader(is));
            }
            for (SyndEntry entry : feed.getEntries()) {
                String link = entry.getLink();
                if (link == null || articleRepository.existsByLink(link)) {
                    continue;
                }
                LocalDateTime publishedAt = null;
                if (entry.getPublishedDate() != null) {
                    publishedAt = entry.getPublishedDate().toInstant()
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDateTime();
                }
                String description = entry.getDescription() != null ? entry.getDescription().getValue() : null;
                String author = entry.getAuthor();
                if (author == null || author.isBlank()) {
                    author = source.getDisplayName();
                }

                String category = source.getCategory() != null
                        ? source.getCategory()
                        : extractCategory(entry.getCategories());

                articleRepository.save(Article.builder()
                        .title(entry.getTitle())
                        .link(link)
                        .description(description)
                        .author(author)
                        .category(category)
                        .imageUrl(extractImageUrl(entry))
                        .source(source)
                        .publishedAt(publishedAt)
                        .build());
                saved++;
            }
            log.info("[RSS] {} - {}건 저장", source.getDisplayName(), saved);
        } catch (Exception e) {
            log.error("[RSS] {} 수집 실패: {}", source.getDisplayName(), e.getMessage());
        }
        return saved;
    }

    private String extractCategory(List<SyndCategory> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return categories.get(0).getName();
    }

    private String extractImageUrl(SyndEntry entry) {
        try {
            if (!entry.getEnclosures().isEmpty()) {
                String type = entry.getEnclosures().get(0).getType();
                if (type != null && type.startsWith("image/")) {
                    return entry.getEnclosures().get(0).getUrl();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
