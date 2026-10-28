package com.example.neeews.huggingface.service;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.huggingface.dto.response.PaperResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaperService {

    private static final int PAGE_SIZE = 20;
    private static final int TOP_SIZE = 10;
    // 기본 응답은 최근 이틀 중 50건만 준다. 이틀 치를 다 받아 추천 순으로 고른다.
    private static final int RECENT_FETCH_SIZE = 100;
    // HF API 한도가 5분에 500회라, 같은 페이지를 여러 사람이 열어도 HF는 한 번만 부른다.
    // 지난 기간도 추천 수는 조금씩 바뀌어서 오래 두지 않는다.
    private static final Duration PAGE_CACHE_TTL = Duration.ofMinutes(10);
    private static final int PAGE_CACHE_MAX_ENTRIES = 500;

    private static final String PERIOD_DATE = "date";
    private static final String PERIOD_WEEK = "week";
    private static final String PERIOD_MONTH = "month";
    private static final Pattern ISO_WEEK = Pattern.compile("\\d{4}-W\\d{2}");

    private final HuggingFaceClient huggingFaceClient;

    private final AtomicReference<List<PaperResponse>> recentTop = new AtomicReference<>(List.of());
    private final Cache<String, Slice<PaperResponse>> pages = Caffeine.newBuilder()
            .expireAfterWrite(PAGE_CACHE_TTL)
            .maximumSize(PAGE_CACHE_MAX_ENTRIES)
            .build();

    // 그날 목록은 하루 동안 채워져서, 오전에는 갓 올라온 추천 0~1개짜리 논문뿐이다.
    // 최근 이틀 치를 추천 순으로 골라야 하루 중 언제 봐도 볼 만한 논문이 위에 온다.
    public void refresh() {
        List<PaperResponse> top = huggingFaceClient.findPapers(null, null, 0, RECENT_FETCH_SIZE).stream()
                .filter(PaperService::isValid)
                .sorted(Comparator.comparingInt((HuggingFaceClient.DailyPaper item) -> item.paper().upvotes()).reversed())
                .limit(TOP_SIZE)
                .map(PaperResponse::from)
                .toList();
        // HF 조회가 실패하면 빈 목록이 온다. 직전 목록을 지우지 않고 다음 주기까지 그대로 보여준다.
        if (!top.isEmpty()) recentTop.set(top);
    }

    public Slice<PaperResponse> getLatestPapers(int page) {
        return fetchPage(null, null, page);
    }

    public Slice<PaperResponse> getDailyPapers(String date, int page) {
        if (date == null) {
            return new SliceImpl<>(recentTop.get(), PageRequest.of(0, TOP_SIZE), false);
        }
        LocalDate day = parse(() -> LocalDate.parse(date), "날짜는 '2026-10-20' 형식으로 지정해야 합니다.");
        rejectFuture(day);
        return fetchPage(PERIOD_DATE, day.toString(), page);
    }

    public Slice<PaperResponse> getWeeklyPapers(String week, int page) {
        String message = "주는 '2026-W43' 형식(ISO 주차)으로 지정해야 합니다.";
        if (week == null || !ISO_WEEK.matcher(week).matches()) {
            throw new IllegalArgumentException(message);
        }
        LocalDate monday = parse(() -> LocalDate.parse(week + "-1", DateTimeFormatter.ISO_WEEK_DATE), message);
        rejectFuture(monday);
        return fetchPage(PERIOD_WEEK, week, page);
    }

    public Slice<PaperResponse> getMonthlyPapers(String month, int page) {
        String message = "월은 '2026-09' 형식으로 지정해야 합니다.";
        if (month == null) {
            throw new IllegalArgumentException(message);
        }
        YearMonth yearMonth = parse(() -> YearMonth.parse(month), message);
        rejectFuture(yearMonth.atDay(1));
        return fetchPage(PERIOD_MONTH, yearMonth.toString(), page);
    }

    private Slice<PaperResponse> fetchPage(String period, String value, int page) {
        if (page < 1) {
            throw new IllegalArgumentException("페이지는 1 이상이어야 합니다.");
        }
        String key = (period == null ? "latest" : period + "=" + value) + "&page=" + page;
        Slice<PaperResponse> cached = pages.getIfPresent(key);
        if (cached != null) return cached;

        List<HuggingFaceClient.DailyPaper> items = huggingFaceClient.findPapers(period, value, page - 1, PAGE_SIZE);
        // HF가 전체 건수를 주지 않아, 한 페이지가 꽉 찼으면 다음 페이지가 있다고 본다.
        Slice<PaperResponse> slice = new SliceImpl<>(
                items.stream().filter(PaperService::isValid).map(PaperResponse::from).toList(),
                PageRequest.of(page - 1, PAGE_SIZE),
                items.size() == PAGE_SIZE);
        // HF 조회가 실패하면 빈 목록이 온다. 빈 결과까지 담아 두면 장애가 풀린 뒤에도 TTL 동안 빈 화면이 나간다.
        if (!items.isEmpty()) pages.put(key, slice);
        return slice;
    }

    private static boolean isValid(HuggingFaceClient.DailyPaper item) {
        return item.paper() != null && item.paper().id() != null;
    }

    private static <T> T parse(Supplier<T> parser, String message) {
        try {
            return parser.get();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void rejectFuture(LocalDate start) {
        if (start.isAfter(PublishedTime.today())) {
            throw new IllegalArgumentException("아직 오지 않은 기간의 논문은 조회할 수 없습니다.");
        }
    }
}
