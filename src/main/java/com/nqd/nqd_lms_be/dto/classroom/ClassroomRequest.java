package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String name;

    private String code;

    private String description;

    private String gradeLevel;

    private UUID subjectId;

    private String coverImageUrl;
}
