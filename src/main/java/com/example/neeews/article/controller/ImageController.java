package com.example.neeews.article.controller;

import com.example.neeews.article.service.ExternalImageFetcher;
import lombok.RequiredArgsConstructor;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ExternalImageFetcher externalImageFetcher;

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
            @RequestParam(required = false) Integer w) {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ResponseEntity.badRequest().build();
        }
        if (w != null && (w < 1 || w > 4000)) {
            return ResponseEntity.badRequest().build();
        }

        try {
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
            Path originalFile = null;
            String ext = null;
            for (String candidate : new String[]{"jpg", "png", "webp", "gif"}) {
                Path file = proxyDir.resolve(hash + "." + candidate);
                if (Files.exists(file)) {
                    originalFile = file;
                    ext = candidate;
                    break;
                }
            }

            // 원본 없으면 다운로드
            if (originalFile == null) {
                ExternalImageFetcher.FetchedImage result = externalImageFetcher.fetch(url);
                if (result == null) {
                    log.warn("[이미지 프록시] 다운로드 실패, 502 반환: {}", url);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                ext = result.ext();
                Files.createDirectories(proxyDir);
                originalFile = proxyDir.resolve(hash + "." + ext);
                Files.write(originalFile, result.bytes());
            }

            boolean resizable = !"webp".equals(ext) && !"gif".equals(ext);

            if (w != null && resizable) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.of(originalFile.toFile())
                        .width(w).keepAspectRatio(true).outputQuality(0.85).toOutputStream(baos);
                byte[] resizedBytes = baos.toByteArray();
                Path cacheDir = Paths.get(imageStoragePath, "cache", String.valueOf(w));
                Files.createDirectories(cacheDir);
                Files.write(cacheDir.resolve(hash + ".jpg"), resizedBytes);
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(cacheControl).body(resizedBytes);
            }

            return ResponseEntity.ok().contentType(resolveMediaType("x." + ext))
                    .cacheControl(cacheControl).body(Files.readAllBytes(originalFile));

        } catch (Exception e) {
            log.error("[이미지 프록시] 처리 중 오류 url={}: {}", url, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
