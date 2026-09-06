package com.example.neeews.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScheduleResponse {

    private LocalDate date;
    private String event;
    private String content;
    private List<Integer> grades;
    private boolean holiday;

    public static ScheduleResponse of(LocalDate date, String event, String content,
                                      List<Integer> grades, boolean holiday) {
        return ScheduleResponse.builder()
                .date(date)
                .event(event)
                .content(content)
                .grades(grades)
                .holiday(holiday)
                .build();
    }
}
