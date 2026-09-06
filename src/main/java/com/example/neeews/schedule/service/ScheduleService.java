package com.example.neeews.schedule.service;

import com.example.neeews.schedule.dto.response.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int MIN_GRADE = 1;
    private static final int MAX_GRADE = 3;
    private static final int MAX_CACHE_ENTRIES = 64;

    private final NeisClient neisClient;

    // 학사일정은 학기 중에 거의 바뀌지 않는다. 공개 엔드포인트라 매 요청마다 NEIS를 때리지 않도록 기간별로 캐싱한다.
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Value("${app.neis.cache-ttl-minutes}")
    private long cacheTtlMinutes;

    public List<ScheduleResponse> getSchedules(LocalDate from, LocalDate to, Integer grade) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("조회 종료일이 시작일보다 빠릅니다.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("한 번에 조회할 수 있는 기간은 " + MAX_RANGE_DAYS + "일까지입니다.");
        }
        if (grade != null && (grade < MIN_GRADE || grade > MAX_GRADE)) {
            throw new IllegalArgumentException("학년은 " + MIN_GRADE + "부터 " + MAX_GRADE + "까지만 조회할 수 있습니다.");
        }

        List<ScheduleResponse> schedules = fetchCached(start, end);
        if (grade == null) {
            return schedules;
        }
        return schedules.stream()
                .filter(schedule -> schedule.getGrades().contains(grade))
                .toList();
    }

    private List<ScheduleResponse> fetchCached(LocalDate from, LocalDate to) {
        String key = from + "~" + to;
        Cached cached = cache.get(key);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.schedules();
        }

        List<ScheduleResponse> schedules = neisClient.fetchSchedules(from, to);
        // 기간을 마음대로 넣을 수 있는 공개 엔드포인트라 키가 무한정 늘 수 있다. 넘치면 통째로 비운다.
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(key, new Cached(schedules, Instant.now()));
        return schedules;
    }

    private record Cached(List<ScheduleResponse> schedules, Instant cachedAt) {

        boolean isExpired(long ttlMinutes) {
            return cachedAt.plus(ttlMinutes, ChronoUnit.MINUTES).isBefore(Instant.now());
        }
    }
}
