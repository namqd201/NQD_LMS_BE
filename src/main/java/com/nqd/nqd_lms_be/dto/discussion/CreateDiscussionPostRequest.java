package com.nqd.nqd_lms_be.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscussionPostRequest {

    @NotBlank(message = "Nội dung câu trả lời không được để trống")
    private String content;

    private java.util.UUID parentId;

    private java.util.List<java.util.UUID> mentionedUserIds;
}
