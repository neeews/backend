package com.example.neeews.huggingface.service;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.huggingface.domain.ModelRelease;
import com.example.neeews.huggingface.dto.response.ModelResponse;
import com.example.neeews.huggingface.repository.ModelReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelReleaseService {

    private static final int PAGE_SIZE = 20;
    // 첫 수집 때 조직마다 몇 달 치 모델이 한꺼번에 '새 모델'로 들어오지 않게 한다.
    private static final int RELEASE_WINDOW_DAYS = 30;

    private final HuggingFaceClient huggingFaceClient;
    private final ModelReleaseRepository modelReleaseRepository;

    @Value("#{'${app.huggingface.watched-orgs}'.split(',')}")
    private List<String> watchedOrgs;

    // 막 올라온 모델은 좋아요가 0이라 출시작인지 연구 부산물인지 아직 모른다.
    // 일단 다 담아 두고 수집 때마다 좋아요를 갱신해, 기준을 넘은 것만 보여준다.
    @Value("${app.huggingface.release-min-likes}")
    private long minLikes;

    // 저장은 건별로 커밋한다. 한 조직 조회가 실패하면 빈 목록이 와서 나머지 조직은 계속 받는다.
    public int collect() {
        return watchedOrgs.stream().mapToInt(this::collectOrg).sum();
    }

    @Transactional(readOnly = true)
    public Page<ModelResponse> getReleases(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("페이지는 1 이상이어야 합니다.");
        }
        return modelReleaseRepository
                .findByLikesGreaterThanEqualOrderByReleasedAtDesc(minLikes, PageRequest.of(page - 1, PAGE_SIZE))
                .map(ModelResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ModelResponse> getNewlyVisibleSince(LocalDateTime since) {
        return modelReleaseRepository.findByVisibleAtAfterOrderByLikesDesc(since).stream()
                .map(ModelResponse::from)
                .toList();
    }

    private int collectOrg(String org) {
        List<HuggingFaceClient.ModelInfo> models = huggingFaceClient.findLatestModels(org).stream()
                .filter(model -> !model.isQuantized() && model.createdAt() != null)
                .toList();
        if (models.isEmpty()) return 0;

        Map<String, ModelRelease> known = modelReleaseRepository
                .findByModelIdIn(models.stream().map(HuggingFaceClient.ModelInfo::id).toList()).stream()
                .collect(Collectors.toMap(ModelRelease::getModelId, Function.identity()));
        LocalDateTime cutoff = PublishedTime.now().minusDays(RELEASE_WINDOW_DAYS);

        int added = 0;
        for (HuggingFaceClient.ModelInfo model : models) {
            // 모델 하나의 값이 컬럼 길이를 넘는 등으로 저장에 실패해도, 그 조직의 나머지와 다음 조직은 계속 받는다.
            try {
                if (upsert(model, known.get(model.id()), cutoff)) added++;
            } catch (Exception e) {
                log.warn("[HuggingFace] 모델 저장 실패 model={}", model.id(), e);
            }
        }
        return added;
    }

    // 새로 담았으면 true. 이미 담은 모델은 좋아요와 라이선스만 갱신한다.
    private boolean upsert(HuggingFaceClient.ModelInfo model, ModelRelease known, LocalDateTime cutoff) {
        boolean visible = model.likes() >= minLikes;
        if (known != null) {
            known.updatePopularity(model.likes(), model.downloads());
            known.updateLicense(model.licenseId(), model.licenseName(), model.licenseUrl());
            if (visible) known.markVisible(PublishedTime.now());
            modelReleaseRepository.save(known);
            return false;
        }
        LocalDateTime releasedAt = LocalDateTime.ofInstant(model.createdAt(), PublishedTime.ZONE);
        if (releasedAt.isBefore(cutoff)) return false;

        modelReleaseRepository.save(ModelRelease.builder()
                .modelId(model.id())
                .author(model.author())
                .pipelineTag(model.pipelineTag())
                .parameterCount(model.parameterCount())
                .baseModel(model.baseModel())
                .licenseId(model.licenseId())
                .licenseName(model.licenseName())
                .licenseUrl(model.licenseUrl())
                .likes(model.likes())
                .downloads(model.downloads())
                .releasedAt(releasedAt)
                .visibleAt(visible ? PublishedTime.now() : null)
                .build());
        return true;
    }
}
