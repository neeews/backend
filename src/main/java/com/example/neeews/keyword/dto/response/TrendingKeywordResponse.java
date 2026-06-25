package com.example.neeews.keyword.dto.response;

import com.example.neeews.keyword.domain.TrendingKeyword;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrendingKeywordResponse {

    private int rank;
    private String word;
    private String change;

    public static TrendingKeywordResponse from(TrendingKeyword keyword) {
        return TrendingKeywordResponse.builder()
                .rank(keyword.getRank())
                .word(keyword.getWord())
                .change(keyword.getChange().name().toLowerCase())
                .build();
    }
}
