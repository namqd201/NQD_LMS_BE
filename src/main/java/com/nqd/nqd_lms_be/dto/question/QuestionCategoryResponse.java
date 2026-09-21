package com.nqd.nqd_lms_be.dto.question;

import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
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
public class QuestionCategoryResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private String gradeLevel;
    private UUID parentId;
    private String parentName;
    private Integer displayOrder;
    private UUID creatorId;
    private String creatorName;
    private QuestionCategoryVisibility visibility;
    private Boolean isSystem;
    private Long questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
