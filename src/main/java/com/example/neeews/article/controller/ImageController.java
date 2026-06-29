package com.example.neeews.article.controller;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

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

    private MediaType resolveMediaType(String filename) {
        if (filename.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (filename.endsWith(".gif"))  return MediaType.IMAGE_GIF;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
