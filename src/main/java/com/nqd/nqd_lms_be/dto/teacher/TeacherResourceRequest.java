package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResourceRequest {

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;

    @NotBlank(message = "Resource title is required")
    private String title;

    @NotBlank(message = "Resource URL is required")
    private String url;

    private String metadata;
    private Integer displayOrder;
}
