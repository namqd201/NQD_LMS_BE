package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
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
public class StudentCourseDetailResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String gradeLevel;
    private String thumbnailUrl;
    private String subjectName;
    private CourseStatus status;
    private Boolean isPrivate;
    private UUID creatorId;
    private String creatorName;
    private boolean isOwner;
    private boolean isEnrolled;
    private List<StudentChapterResponse> chapters;
}
