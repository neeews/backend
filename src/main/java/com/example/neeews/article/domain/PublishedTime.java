package com.example.neeews.article.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 서버·DB는 UTC로 돌지만 publishedAt만 한국 시간으로 저장한다(화면에 그대로 보여주기 위해).
// publishedAt과 비교하는 기준 시각을 LocalDateTime.now()로 잡으면 9시간 어긋나므로 반드시 여기서 얻는다.
public final class PublishedTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private PublishedTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime startOfToday() {
        return today().atStartOfDay();
    }
}
