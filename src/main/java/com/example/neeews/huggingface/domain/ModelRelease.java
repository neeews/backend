package com.example.neeews.huggingface.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_releases", indexes = {
        @Index(name = "idx_model_release_model_id", columnList = "modelId", unique = true),
        @Index(name = "idx_model_release_released_at", columnList = "releasedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ModelRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String modelId;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 50)
    private String pipelineTag;

    private Long parameterCount;

    @Column(length = 200)
    private String baseModel;

    @Column(length = 50)
    private String licenseId;

    @Column(length = 255)
    private String licenseName;

    @Column(length = 1000)
    private String licenseUrl;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false)
    private long likes;

    @Column(nullable = false)
    private long downloads;

    // HF가 저장소를 만든 시각. 기사 publishedAt과 같이 한국 시간으로 저장해 화면에 그대로 쓴다.
    @Column(nullable = false)
    private LocalDateTime releasedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fetchedAt;

    // 좋아요가 노출 기준을 처음 넘은 시각(한국 시간). 브리핑 메일이 "새로 뜬 모델"을 하루 한 번만 싣는 데 쓴다.
    private LocalDateTime visibleAt;

    @PrePersist
    void onCreate() {
        this.fetchedAt = LocalDateTime.now();
    }

    public void updatePopularity(long likes, long downloads) {
        this.likes = likes;
        this.downloads = downloads;
    }

    // 공개 뒤에 라이선스를 바꾸는 모델이 있다. 상업 이용 표시가 낡지 않도록 수집 때마다 덮어쓴다.
    public void updateLicense(String licenseId, String licenseName, String licenseUrl) {
        this.licenseId = licenseId;
        this.licenseName = licenseName;
        this.licenseUrl = licenseUrl;
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public void markVisible(LocalDateTime at) {
        if (this.visibleAt == null) {
            this.visibleAt = at;
        }
    }
}
