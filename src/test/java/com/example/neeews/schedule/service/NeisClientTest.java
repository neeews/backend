package com.example.neeews.schedule.service;

import com.example.neeews.schedule.dto.response.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NeisClientTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final String EXPECTED_URL = "https://open.neis.go.kr/hub/SchoolSchedule"
            + "?Type=json&pIndex=1&pSize=1000&ATPT_OFCDC_SC_CODE=F10&SD_SCHUL_CODE=7140392"
            + "&AA_FROM_YMD=20260901&AA_TO_YMD=20260930";

    private MockRestServiceServer server;
    private NeisClient neisClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://open.neis.go.kr");
        server = MockRestServiceServer.bindTo(builder).build();
        neisClient = new NeisClient(builder.build(), "", "F10", "7140392");
    }

    @Test
    @DisplayName("학년별 대상 여부와 휴업일 여부를 응답에 담는다")
    void 정상_응답을_파싱한다() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("""
                        {"SchoolSchedule":[
                          {"head":[{"list_total_count":2},{"RESULT":{"CODE":"INFO-000","MESSAGE":"정상 처리되었습니다."}}]},
                          {"row":[
                            {"AA_YMD":"20260905","EVENT_NM":"토요휴업일","EVENT_CNTNT":"",
                             "ONE_GRADE_EVENT_YN":"Y","TW_GRADE_EVENT_YN":"Y","THREE_GRADE_EVENT_YN":"Y",
                             "SBTR_DD_SC_NM":"휴업일"},
                            {"AA_YMD":"20260908","EVENT_NM":"영어듣기평가","EVENT_CNTNT":"1학년 영어듣기평가",
                             "ONE_GRADE_EVENT_YN":"Y","TW_GRADE_EVENT_YN":"N","THREE_GRADE_EVENT_YN":"N",
                             "SBTR_DD_SC_NM":"해당없음"}
                          ]}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<ScheduleResponse> schedules = neisClient.fetchSchedules(FROM, TO);

        assertThat(schedules).hasSize(2);
        assertThat(schedules.get(0).getDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(schedules.get(0).getEvent()).isEqualTo("토요휴업일");
        assertThat(schedules.get(0).getContent()).isNull();
        assertThat(schedules.get(0).getGrades()).containsExactly(1, 2, 3);
        assertThat(schedules.get(0).isHoliday()).isTrue();

        assertThat(schedules.get(1).getContent()).isEqualTo("1학년 영어듣기평가");
        assertThat(schedules.get(1).getGrades()).containsExactly(1);
        assertThat(schedules.get(1).isHoliday()).isFalse();
    }

    @Test
    @DisplayName("일정이 없는 기간은 오류가 아니라 빈 목록이다")
    void 데이터가_없으면_빈_목록을_준다() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(
                        """
                        {"RESULT":{"CODE":"INFO-200","MESSAGE":"해당하는 데이터가 없습니다."}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(neisClient.fetchSchedules(FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("생성자가 둘이라도 스프링이 주입용 생성자를 고른다")
    void 빈으로_등록된다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.neis.base-url", "https://open.neis.go.kr")
                .withProperty("app.neis.api-key", "")
                .withProperty("app.neis.office-code", "F10")
                .withProperty("app.neis.school-code", "7140392");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(PropertySourcesPlaceholderConfigurer.class, NeisClient.class);
            context.refresh();

            assertThat(context.getBean(NeisClient.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("NEIS가 오류 코드를 주면 예외로 알린다")
    void 오류_코드는_예외로_바뀐다() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(
                        """
                        {"RESULT":{"CODE":"ERROR-300","MESSAGE":"필수 값이 누락되어 있습니다."}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> neisClient.fetchSchedules(FROM, TO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ERROR-300");
    }
}
