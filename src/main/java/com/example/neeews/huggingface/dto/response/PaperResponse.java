package com.example.neeews.huggingface.dto.response;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.huggingface.service.HuggingFaceClient;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaperResponse {

    private static final String PAPER_URL_PREFIX = "https://huggingface.co/papers/";
    // ai_summary가 없는 논문(100건 중 2건)은 초록으로 대신한다. 초록은 1,000자를 넘기 일쑤라 카드에 맞게 자른다.
    private static final int MAX_ABSTRACT_LENGTH = 300;

    private String id;
    private String title;
    private String summary;
    private int upvotes;
    private String thumbnail;
    private LocalDateTime publishedAt;
    // HF Daily Papers에 올라온 시각. 날짜·주·월별 조회와 최신순 정렬이 이 시각 기준이라 arXiv 공개일(publishedAt)과 며칠씩 다르다.
    private LocalDateTime featuredAt;
    private String url;

    public static PaperResponse from(HuggingFaceClient.DailyPaper item) {
        HuggingFaceClient.Paper paper = item.paper();
        return PaperResponse.builder()
                .id(paper.id())
                .title(item.title())
                .summary(summarize(paper))
                .upvotes(paper.upvotes())
                .thumbnail(item.thumbnail())
                .publishedAt(toKst(paper.publishedAt()))
                .featuredAt(toKst(paper.submittedOnDailyAt()))
                .url(PAPER_URL_PREFIX + paper.id())
                .build();
    }

    private static String summarize(HuggingFaceClient.Paper paper) {
        if (paper.aiSummary() != null && !paper.aiSummary().isBlank()) return paper.aiSummary().trim();
        if (paper.summary() == null) return null;
        String plain = paper.summary().replaceAll("\\s+", " ").trim();
        return plain.length() > MAX_ABSTRACT_LENGTH ? plain.substring(0, MAX_ABSTRACT_LENGTH) + "..." : plain;
    }

    private static LocalDateTime toKst(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, PublishedTime.ZONE);
    }
}
