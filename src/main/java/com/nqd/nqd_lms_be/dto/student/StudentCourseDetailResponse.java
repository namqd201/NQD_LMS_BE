package com.nqd.nqd_lms_be.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isOwner")
    private boolean isOwner;

    @JsonProperty("isEnrolled")
    private boolean isEnrolled;

    private List<StudentChapterResponse> chapters;

    @JsonProperty("owner")
    public boolean getOwner() {
        return isOwner;
    }

    @JsonProperty("enrolled")
    public boolean getEnrolled() {
        return isEnrolled;
    }
}
