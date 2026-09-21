package com.nqd.nqd_lms_be.dto;

import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApplicationResponse {
    private UUID id;
    private UUID userId;
    private TeacherApplicantType applicantType;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String institutionName;
    private String majorOrSubject;
    private String bio;
    private List<String> documentUrls;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String sampleVideoUrl;
    private TeacherApplicationStatus status;
    private String rejectReason;
    private UUID reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
