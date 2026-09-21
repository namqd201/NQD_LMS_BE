package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResourceResponse {
    private UUID id;
    private UUID lessonId;
    private ResourceType resourceType;
    private String title;
    private String url;
    private String metadata;
    private Integer displayOrder;
    private Boolean isPreview;
}
