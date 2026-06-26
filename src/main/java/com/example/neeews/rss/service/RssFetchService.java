package com.example.neeews.rss.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.rss.domain.NewsSource;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFetchService {

    private final ArticleRepository articleRepository;

    private static final Set<NewsSource> DEPRECATED_SOURCES = Set.of(
            NewsSource.YONHAP, NewsSource.DONGA, NewsSource.KHAN
    );

    private static final Map<String, String> CONTENT_SELECTORS = Map.of(
        "연합뉴스", "article.story-news",
        "한겨레", "div.article-text",
        "경향신문", "div.art_body",
        "한국경제", "div#articletxt",
        "전자신문", "div.article_txt",
        "ZDnet코리아", "div#article_body"
    );

    @Transactional
    public int fetchAll() {
        return Arrays.stream(NewsSource.values())
                .filter(s -> !DEPRECATED_SOURCES.contains(s))
                .mapToInt(this::fetchSource)
                .sum();
    }

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
        Map.entry("정치일반", "정치"),
        Map.entry("청와대", "정치"),
        Map.entry("국방·외교", "정치"),
        Map.entry("경제 일반", "경제"),
        Map.entry("금융·재테크", "경제"),
        Map.entry("산업·통상", "경제"),
        Map.entry("사회 일반", "사회"),
        Map.entry("사회일반", "사회"),
        Map.entry("사건·사고", "사회"),
        Map.entry("보건·복지", "사회"),
        Map.entry("노동", "사회"),
        Map.entry("교육·입시", "사회"),
        Map.entry("젠더", "사회"),
        Map.entry("법원·검찰", "사회"),
        Map.entry("국제 일반", "세계"),
        Map.entry("미국·중남미", "세계"),
        Map.entry("중동·아프리카", "세계"),
        Map.entry("문화 일반", "연예/문화"),
        Map.entry("책", "연예/문화"),
        Map.entry("월드컵", "스포츠"),
        Map.entry("사설", "종합"),
        Map.entry("인물일반", "종합")
    );

    public List<Map<String, Object>> getCategoryStats() {
        return articleRepository.findCategoryStats().stream()
                .map(row -> Map.<String, Object>of("category", row[0] == null ? "(null)" : row[0], "count", row[1]))
                .toList();
    }

    @Transactional
    public int normalizeCategories() {
        List<Article> all = articleRepository.findAll();
        int updated = 0;
        for (Article article : all) {
            String current = article.getCategory();
            String correct = current != null
                    ? CATEGORY_MAP.getOrDefault(current, article.getSource().getCategory())
                    : article.getSource().getCategory();
            if (correct != null && !correct.equals(current)) {
                article.updateCategory(correct);
                updated++;
            }
        }
        log.info("[카테고리 정규화] 전체 {}건 중 {}건 수정", all.size(), updated);
        return updated;
    }

    @Transactional
    public int fetchSource(NewsSource source) {
        int saved = 0;
        try {
            HttpURLConnection conn = openConnection(source.getRssUrl());
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

    public String crawlArticleContent(String url, String sourceName) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; NeeewsBot/1.0)")
                    .timeout(10_000)
                    .get();
            String selector = CONTENT_SELECTORS.get(sourceName);
            Element body = selector != null ? doc.selectFirst(selector) : null;
            if (body == null) body = doc.selectFirst("article");
            if (body == null) body = doc.selectFirst("main");
            if (body == null) return null;
            String text = body.select("p").stream()
                    .map(Element::text)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n\n"));
            if (text.isBlank()) text = body.text().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("[본문 크롤링] 실패 url={}: {}", url, e.getMessage());
            return null;
        }
    }

    public String crawlImageUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; NeeewsBot/1.0)");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(10_000);
            conn.setInstanceFollowRedirects(true);
            try (InputStream is = conn.getInputStream()) {
                String html = new String(is.readNBytes(32_768), java.nio.charset.StandardCharsets.UTF_8);
                Matcher og = OG_IMAGE_PATTERN.matcher(html);
                if (og.find()) return og.group(1) != null ? og.group(1) : og.group(2);
                Matcher img = IMG_PATTERN.matcher(html);
                if (img.find()) return img.group(1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private HttpURLConnection openConnection(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; NeeewsBot/1.0)");
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        int status = conn.getResponseCode();
        if (status == 301 || status == 302 || status == 308) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; NeeewsBot/1.0)");
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
        }
        return conn;
    }

    private String extractCategory(List<SyndCategory> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return categories.get(0).getName();
    }

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_PATTERN = Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']|<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);

    private String extractImageUrl(SyndEntry entry) {
        // 1. <enclosure type="image/...">
        try {
            for (var enc : entry.getEnclosures()) {
                if (enc.getType() != null && enc.getType().startsWith("image/")) {
                    return enc.getUrl();
                }
            }
        } catch (Exception ignored) {}

        // 2. <media:thumbnail> 또는 <media:content>
        try {
            MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
            if (media != null) {
                for (var group : media.getMediaGroups()) {
                    var thumbs = group.getMetadata().getThumbnail();
                    if (thumbs != null && thumbs.length > 0) return thumbs[0].getUrl().toString();
                    for (var content : group.getContents()) {
                        if (content.getReference() != null) return content.getReference().toString();
                    }
                }
                for (var thumb : media.getMetadata().getThumbnail()) {
                    return thumb.getUrl().toString();
                }
                for (var content : media.getMediaContents()) {
                    if (content.getReference() != null) return content.getReference().toString();
                }
            }
        } catch (Exception ignored) {}

        // 3. <description> HTML에서 <img src="..."> 추출
        try {
            String desc = entry.getDescription() != null ? entry.getDescription().getValue() : null;
            if (desc != null) {
                Matcher m = IMG_PATTERN.matcher(desc);
                if (m.find()) return m.group(1);
            }
        } catch (Exception ignored) {}

        return null;
    }
}
