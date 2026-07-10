package com.example.neeews.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// app.scheduling.enabled=false로 끌 수 있게 분리 — CI 테스트에서 RSS 수집 스케줄러가 실제 언론사에 요청하지 않도록
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
