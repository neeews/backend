package com.example.neeews.huggingface.dto.response;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.huggingface.domain.CommercialUse;
import com.example.neeews.huggingface.domain.ModelRelease;
import com.example.neeews.huggingface.service.HuggingFaceClient;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ModelResponse {

    private static final String MODEL_URL_PREFIX = "https://huggingface.co/";

    private String id;
    private String name;
    private String author;
    private String pipelineTag;
    private Long parameterCount;
    private String baseModel;
    private String license;
    private String licenseUrl;
    private CommercialUse commercialUse;
    private String summary;
    private long likes;
    private long downloads;
    private LocalDateTime releasedAt;
    private String url;

    public static ModelResponse from(ModelRelease release) {
        return ModelResponse.builder()
                .id(release.getModelId())
                .name(nameOf(release.getModelId()))
                .author(release.getAuthor())
                .pipelineTag(release.getPipelineTag())
                .parameterCount(release.getParameterCount())
                .baseModel(release.getBaseModel())
                .license(licenseOf(release.getLicenseName(), release.getLicenseId()))
                .licenseUrl(release.getLicenseUrl())
                .commercialUse(CommercialUse.of(release.getLicenseId()))
                .summary(release.getAiSummary())
                .likes(release.getLikes())
                .downloads(release.getDownloads())
                .releasedAt(release.getReleasedAt())
                .url(MODEL_URL_PREFIX + release.getModelId())
                .build();
    }

    // 트렌딩은 30분마다 바뀌는 목록이라 저장하지 않아 한국어 요약이 없다.
    public static ModelResponse from(HuggingFaceClient.ModelInfo model) {
        return ModelResponse.builder()
                .id(model.id())
                .name(nameOf(model.id()))
                .author(model.author())
                .pipelineTag(model.pipelineTag())
                .parameterCount(model.parameterCount())
                .baseModel(model.baseModel())
                .license(licenseOf(model.licenseName(), model.licenseId()))
                .licenseUrl(model.licenseUrl())
                .commercialUse(CommercialUse.of(model.licenseId()))
                .likes(model.likes())
                .downloads(model.downloads())
                .releasedAt(model.createdAt() == null ? null : LocalDateTime.ofInstant(model.createdAt(), PublishedTime.ZONE))
                .url(MODEL_URL_PREFIX + model.id())
                .build();
    }

    private static String nameOf(String modelId) {
        return modelId.substring(modelId.indexOf('/') + 1);
    }

    // 직접 만든 라이선스는 id가 "other"라 이름("qwen-research")이 있으면 그쪽을 보여준다.
    private static String licenseOf(String licenseName, String licenseId) {
        return licenseName != null ? licenseName : licenseId;
    }
}
