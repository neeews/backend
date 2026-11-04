package com.example.neeews.briefing.controller;

import com.example.neeews.briefing.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/briefing")
@RequiredArgsConstructor
public class BriefingController {

    // 문자셋을 적지 않으면 스프링 기본 변환기가 text/html을 ISO-8859-1로 써서 한글이 '?'로 깨진다.
    private static final String HTML_UTF8 = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    private final BriefingService briefingService;

    // 메일 보안 검사기가 본문 링크를 미리 열어 보는 경우가 있다. GET에서 바로 해지하면 사용자가 누르지도 않았는데
    // 수신 거부가 되므로, GET은 확인 화면만 보여주고 해지는 버튼(POST)으로 한다.
    @GetMapping(value = "/unsubscribe", produces = HTML_UTF8)
    public ResponseEntity<String> confirmUnsubscribe(@RequestParam(required = false) String token) {
        return ResponseEntity.ok(briefingService.unsubscribeConfirmPage(token));
    }

    // 메일 앱의 "구독 취소" 버튼(List-Unsubscribe-Post)도 이 주소로 POST를 보낸다.
    @PostMapping(value = "/unsubscribe", produces = HTML_UTF8)
    public ResponseEntity<String> unsubscribe(@RequestParam(required = false) String token) {
        return ResponseEntity.ok(briefingService.unsubscribe(token));
    }
}
