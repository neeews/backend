package com.example.neeews.admin.controller;

import com.example.neeews.briefing.service.BriefingService;
import com.example.neeews.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/briefing")
@RequiredArgsConstructor
public class AdminBriefingController {

    // 문자셋을 적지 않으면 스프링 기본 변환기가 text/html을 ISO-8859-1로 써서 한글이 '?'로 깨진다.
    private static final String HTML_UTF8 = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    private final BriefingService briefingService;

    @GetMapping(value = "/preview", produces = HTML_UTF8)
    public ResponseEntity<String> preview() {
        return ResponseEntity.ok(briefingService.preview());
    }

    // 미리보기로는 SMTP 계정·스팸함 분류를 알 수 없어, 실제 발송 경로로 관리자 본인에게 한 통 보낸다.
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTest(Authentication authentication) {
        String email = AuthUtils.resolveEmail(authentication);
        briefingService.sendTest(email);
        return ResponseEntity.ok(Map.of("sentTo", email));
    }
}
