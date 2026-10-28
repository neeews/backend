package com.example.neeews.huggingface.service;

import com.example.neeews.huggingface.dto.response.ModelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class TrendingModelService {

    private static final int TRENDING_SIZE = 10;

    private final HuggingFaceClient huggingFaceClient;

    // 요청마다 HF를 부르지 않도록 스케줄러가 받아 둔 목록을 그대로 내준다.
    private final AtomicReference<List<ModelResponse>> trending = new AtomicReference<>(List.of());

    public void refresh() {
        List<ModelResponse> models = huggingFaceClient.findTrendingModels().stream()
                .filter(model -> !model.isQuantized())
                .limit(TRENDING_SIZE)
                .map(ModelResponse::from)
                .toList();
        // HF 조회가 실패하면 빈 목록이 온다. 직전 목록을 지우지 않고 다음 주기까지 그대로 보여준다.
        if (!models.isEmpty()) trending.set(models);
    }

    public List<ModelResponse> getTrending() {
        return trending.get();
    }
}
