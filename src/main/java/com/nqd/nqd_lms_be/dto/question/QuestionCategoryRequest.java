package com.nqd.nqd_lms_be.dto.question;

import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCategoryRequest {

    @NotBlank(message = "Tên chuyên đề / danh mục không được để trống")
    private String name;

    private String code;

    private String description;

    @NotNull(message = "Môn học không được để trống")
    private UUID subjectId;

    private String gradeLevel;

    private UUID parentId;

    private Integer displayOrder;

    private QuestionCategoryVisibility visibility;

    private Boolean isSystem;
}
