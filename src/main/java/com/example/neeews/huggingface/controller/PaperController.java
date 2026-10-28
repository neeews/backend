package com.example.neeews.huggingface.controller;

import com.example.neeews.huggingface.dto.response.PaperResponse;
import com.example.neeews.huggingface.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 기간 파라미터를 required로 두면 빠뜨렸을 때 스프링 예외가 전역 핸들러의 500으로 떨어진다.
// 선택 파라미터로 받고 서비스가 400과 예시 형식을 돌려준다.
@RestController
@RequestMapping("/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLatestPapers(@RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(toBody(paperService.getLatestPapers(page)));
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailyPapers(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(toBody(paperService.getDailyPapers(date, page)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyPapers(
            @RequestParam(required = false) String week,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(toBody(paperService.getWeeklyPapers(week, page)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyPapers(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(toBody(paperService.getMonthlyPapers(month, page)));
    }

    private Map<String, Object> toBody(Slice<PaperResponse> slice) {
        return Map.of(
                "papers", slice.getContent(),
                "page", slice.getNumber() + 1,
                "hasNext", slice.hasNext()
        );
    }
}
