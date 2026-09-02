package com.example.neeews.article.domain;

import lombok.Getter;

@Getter
public enum Importance {

    // enum을 STRING으로 저장해 DB 정렬은 알파벳순(HIGH, LOW, MEDIUM)이 된다. 중요도순 정렬은 weight로 한다.

    HIGH(3, "중요"),
    MEDIUM(2, "보통"),
    LOW(1, "낮음");

    private final int weight;
    private final String displayName;

    Importance(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }
}
