package com.example.neeews.schedule.service;

import com.example.neeews.schedule.dto.response.ScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NeisClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    // NEIS 한 페이지 최대치. 조회 기간을 1년으로 제한해 두어 학사일정은 항상 한 페이지에 담긴다.
    private static final int PAGE_SIZE = 1000;
    private static final String NO_DATA_CODE = "INFO-200";
    private static final String YES = "Y";

    private static final List<String> GRADE_FIELDS =
            List.of("ONE_GRADE_EVENT_YN", "TW_GRADE_EVENT_YN", "THREE_GRADE_EVENT_YN");
    private static final String HOLIDAY = "휴업일";

    private final RestClient restClient;
    private final String apiKey;
    private final String officeCode;
    private final String schoolCode;

    // 테스트용 생성자가 따로 있어 Spring이 어느 쪽을 쓸지 알 수 없다. 주입 대상을 명시한다.
    @Autowired
    public NeisClient(@Value("${app.neis.base-url}") String baseUrl,
                      @Value("${app.neis.api-key}") String apiKey,
                      @Value("${app.neis.office-code}") String officeCode,
                      @Value("${app.neis.school-code}") String schoolCode) {
        this(buildRestClient(baseUrl), apiKey, officeCode, schoolCode);
    }

    NeisClient(RestClient restClient, String apiKey, String officeCode, String schoolCode) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.officeCode = officeCode;
        this.schoolCode = schoolCode;
    }

    private static RestClient buildRestClient(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public List<ScheduleResponse> fetchSchedules(LocalDate from, LocalDate to) {
        JsonNode body = restClient.get()
                .uri(builder -> {
                    builder.path("/hub/SchoolSchedule")
                            .queryParam("Type", "json")
                            .queryParam("pIndex", 1)
                            .queryParam("pSize", PAGE_SIZE)
                            .queryParam("ATPT_OFCDC_SC_CODE", officeCode)
                            .queryParam("SD_SCHUL_CODE", schoolCode)
                            .queryParam("AA_FROM_YMD", from.format(YMD))
                            .queryParam("AA_TO_YMD", to.format(YMD));
                    // 인증키는 없어도 조회된다. 호출량이 늘어 제한에 걸릴 때만 발급받아 넣는다.
                    if (!apiKey.isBlank()) {
                        builder.queryParam("KEY", apiKey);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(JsonNode.class);

        if (body == null) {
            throw new IllegalStateException("NEIS 학사일정 응답이 비어 있습니다.");
        }

        // 조회 결과가 없거나 요청이 잘못되면 SchoolSchedule 없이 RESULT만 온다.
        JsonNode result = body.get("RESULT");
        if (result != null) {
            String code = result.path("CODE").asString();
            if (NO_DATA_CODE.equals(code)) {
                return List.of();
            }
            throw new IllegalStateException(
                    "NEIS 학사일정 조회 실패: " + code + " " + result.path("MESSAGE").asString());
        }

        List<ScheduleResponse> schedules = new ArrayList<>();
        for (JsonNode block : body.path("SchoolSchedule")) {
            for (JsonNode row : block.path("row")) {
                schedules.add(toResponse(row));
            }
        }
        return schedules;
    }

    private ScheduleResponse toResponse(JsonNode row) {
        List<Integer> grades = new ArrayList<>();
        for (int i = 0; i < GRADE_FIELDS.size(); i++) {
            if (YES.equals(row.path(GRADE_FIELDS.get(i)).asString())) {
                grades.add(i + 1);
            }
        }

        String content = row.path("EVENT_CNTNT").asString().trim();
        return ScheduleResponse.of(
                LocalDate.parse(row.path("AA_YMD").asString(), YMD),
                row.path("EVENT_NM").asString().trim(),
                content.isEmpty() ? null : content,
                grades,
                HOLIDAY.equals(row.path("SBTR_DD_SC_NM").asString()));
    }
}
