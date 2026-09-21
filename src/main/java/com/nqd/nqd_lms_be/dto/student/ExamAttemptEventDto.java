package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptEventDto {
    private UUID id;
    private ExamAttemptEventType eventType;
    private LocalDateTime occurredAt;
    private String metadata;
    private Boolean isViolation;
}
