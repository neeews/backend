package com.example.neeews.article.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PipelineStatusResponse {

    public enum Status {
        UP, WARN, DOWN
    }

    private Status status;
    private LocalDateTime checkedAt;
    private List<StageStatus> stages;

    public static PipelineStatusResponse of(List<StageStatus> stages) {
        return PipelineStatusResponse.builder()
                .status(worstOf(stages))
                .checkedAt(LocalDateTime.now())
                .stages(stages)
                .build();
    }

    // 한 단계라도 막히면 그 뒤가 전부 굶으므로, 전체 상태는 가장 나쁜 단계를 따른다.
    private static Status worstOf(List<StageStatus> stages) {
        if (stages.stream().anyMatch(s -> s.getStatus() == Status.DOWN)) return Status.DOWN;
        if (stages.stream().anyMatch(s -> s.getStatus() == Status.WARN)) return Status.WARN;
        return Status.UP;
    }

    @Getter
    @Builder
    public static class StageStatus {

        private String name;
        private Status status;
        private String message;
        private LocalDateTime lastSuccessAt;
        private Long backlog;

        public static StageStatus of(String name, Status status, String message,
                                     LocalDateTime lastSuccessAt, Long backlog) {
            return StageStatus.builder()
                    .name(name)
                    .status(status)
                    .message(message)
                    .lastSuccessAt(lastSuccessAt)
                    .backlog(backlog)
                    .build();
        }
    }
}
