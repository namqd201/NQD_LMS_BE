package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptEventRequest {
    @NotNull(message = "Event type is required")
    private ExamAttemptEventType eventType;

    private LocalDateTime occurredAt;

    private String metadata;

    private Boolean isViolation;
}
