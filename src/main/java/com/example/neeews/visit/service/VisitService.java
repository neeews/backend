package com.example.neeews.visit.service;

import com.example.neeews.visit.domain.DailyVisit;
import com.example.neeews.visit.repository.DailyVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitService {

    // 서버는 UTC로 돈다. 한국 날짜로 나눠야 관리자가 보는 "오늘"이 오전 9시가 아니라 자정에 바뀐다.
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final DailyVisitRepository dailyVisitRepository;

    @Transactional
    public void recordVisit() {
        dailyVisitRepository.increment(LocalDate.now(SEOUL_ZONE));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        long today = dailyVisitRepository.findByVisitDate(LocalDate.now(SEOUL_ZONE))
                .map(DailyVisit::getVisitorCount)
                .orElse(0L);
        return Map.of(
                "today", today,
                "total", dailyVisitRepository.sumVisitorCount()
        );
    }
}
