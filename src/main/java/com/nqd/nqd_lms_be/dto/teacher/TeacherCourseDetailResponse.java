package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
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
public class TeacherCourseDetailResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private String name;
    private String code;
    private String description;
    private String gradeLevel;
    private String thumbnailUrl;
    private CourseStatus status;
    private Boolean isPrivate;
    private UUID creatorId;
    private String creatorName;
    private List<TeacherChapterResponse> chapters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
