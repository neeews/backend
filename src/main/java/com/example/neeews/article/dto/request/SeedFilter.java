package com.example.neeews.article.dto.request;

public enum SeedFilter {

    ALL,
    // AI가 이미 매긴 기사만 — 시드 라벨이 사람 판단과 얼마나 맞는지 재는 대조군
    SEED_ONLY,
    // AI가 손대지 않은 기사만 — 오염 없는 정답지를 만들 때
    NO_SEED
}
