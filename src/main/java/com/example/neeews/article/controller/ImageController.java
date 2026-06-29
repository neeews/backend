package com.example.neeews.article.controller;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Value("${app.image.storage-path}")
    private String imageStoragePath;

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String filename,
            @RequestParam(required = false) Integer w) throws Exception {

        if (filename.contains("..") || filename.contains("/")) {
            return ResponseEntity.badRequest().build();
        }
        if (w != null && (w < 1 || w > 4000)) {
            return ResponseEntity.badRequest().build();
        }

        Path original = Paths.get(imageStoragePath, filename);
        if (!Files.exists(original)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = resolveMediaType(filename);
        CacheControl cache = CacheControl.maxAge(7, TimeUnit.DAYS);

        // WebP·GIF는 Thumbnailator로 처리 불가 → 원본 반환
        boolean resizable = !filename.endsWith(".webp") && !filename.endsWith(".gif");

        if (w == null || !resizable) {
            return ResponseEntity.ok().contentType(mediaType).cacheControl(cache)
                    .body(Files.readAllBytes(original));
        }

        // 캐시 확인
        Path cacheDir = Paths.get(imageStoragePath, "cache", String.valueOf(w));
        Path cached = cacheDir.resolve(filename);
        if (Files.exists(cached)) {
            return ResponseEntity.ok().contentType(mediaType).cacheControl(cache)
                    .body(Files.readAllBytes(cached));
        }

        // 리사이즈 (비율 유지, 업스케일 없음)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(original.toFile())
                .width(w)
                .allowOverwrite(true)
                .keepAspectRatio(true)
                .outputQuality(0.85)
                .toOutputStream(baos);

        byte[] result = baos.toByteArray();

        // 캐시 저장
        Files.createDirectories(cacheDir);
        Files.write(cached, result);

        return ResponseEntity.ok().contentType(mediaType).cacheControl(cache).body(result);
    }

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(
            @RequestParam String url,
            @RequestParam(required = false) Integer w) throws Exception {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ResponseEntity.badRequest().build();
        }
        if (w != null && (w < 1 || w > 4000)) {
            return ResponseEntity.badRequest().build();
        }

        String hash = urlHash(url);
        Path proxyDir = Paths.get(imageStoragePath, "proxy");
        CacheControl cacheControl = CacheControl.maxAge(7, TimeUnit.DAYS);

        // 리사이즈 캐시 우선 확인
        if (w != null) {
            Path resized = Paths.get(imageStoragePath, "cache", String.valueOf(w), hash + ".jpg");
            if (Files.exists(resized)) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(cacheControl).body(Files.readAllBytes(resized));
            }
        }

        // 원본 캐시 확인
        byte[] imageBytes = null;
        String ext = null;
        for (String candidate : new String[]{"jpg", "png", "webp", "gif"}) {
            Path file = proxyDir.resolve(hash + "." + candidate);
            if (Files.exists(file)) {
                imageBytes = Files.readAllBytes(file);
                ext = candidate;
                break;
            }
        }

        // 원본 없으면 다운로드
        if (imageBytes == null) {
            DownloadResult result = downloadImage(url);
            if (result == null) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            imageBytes = result.bytes();
            ext = result.ext();
            Files.createDirectories(proxyDir);
            Files.write(proxyDir.resolve(hash + "." + ext), imageBytes);
        }

        boolean resizable = !"webp".equals(ext) && !"gif".equals(ext);

        if (w != null && resizable) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .width(w).keepAspectRatio(true).outputQuality(0.85).toOutputStream(baos);
            byte[] resizedBytes = baos.toByteArray();
            Path cacheDir = Paths.get(imageStoragePath, "cache", String.valueOf(w));
            Files.createDirectories(cacheDir);
            Files.write(cacheDir.resolve(hash + ".jpg"), resizedBytes);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(cacheControl).body(resizedBytes);
        }

        return ResponseEntity.ok().contentType(resolveMediaType("x." + ext))
                .cacheControl(cacheControl).body(imageBytes);
    }

    private record DownloadResult(byte[] bytes, String ext) {}

    private DownloadResult downloadImage(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            if (conn instanceof HttpsURLConnection https) {
                SSLContext ctx = SSLContext.getInstance("TLSv1.2");
                ctx.init(null, null, null);
                https.setSSLSocketFactory(ctx.getSocketFactory());
            }
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(15_000);
            conn.setInstanceFollowRedirects(true);

            String contentType = conn.getContentType();
            String ext = "jpg";
            if (contentType != null) {
                if (contentType.contains("png"))  ext = "png";
                else if (contentType.contains("gif"))  ext = "gif";
                else if (contentType.contains("webp")) ext = "webp";
            }
            return new DownloadResult(conn.getInputStream().readAllBytes(), ext);
        } catch (Exception e) {
            log.warn("[이미지 프록시] 다운로드 실패 url={}: {}", url, e.getMessage());
            return null;
        }
    }

    private String urlHash(String url) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(String.format("%02x", hash[i]));
        return sb.toString();
    }

    private MediaType resolveMediaType(String filename) {
        if (filename.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (filename.endsWith(".gif"))  return MediaType.IMAGE_GIF;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
