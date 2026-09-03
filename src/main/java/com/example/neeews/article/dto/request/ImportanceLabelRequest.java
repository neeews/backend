package com.example.neeews.article.dto.request;

import com.example.neeews.article.domain.Importance;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public class ImportanceLabelRequest {

    @NotNull
    private Importance label;

    // 같은 기사를 다시 매기는 회차. 0이 첫 라벨이고, 자기일치율을 잴 때 1을 쓴다.
    @PositiveOrZero
    private int round;
}
