package com.nqd.nqd_lms_be.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateEligibilityResponse {
    private boolean eligible;
    private boolean alreadyIssued;
    private String certificateCode;
    private UUID certificateId;

    private boolean lessonsCompleted;
    private int completedLessons;
    private int totalLessons;

    private boolean videosCompleted;
    private int watchedVideos;
    private int totalVideos;

    private boolean exercisesCompleted;
    private int passedExercises;
    private int totalExercises;

    private boolean examsCompleted;
    private int passedExams;
    private int totalExams;

    private List<String> missingRequirements;
    private String message;
}