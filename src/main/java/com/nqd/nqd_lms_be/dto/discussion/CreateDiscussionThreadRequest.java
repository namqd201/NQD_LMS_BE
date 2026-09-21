package com.nqd.nqd_lms_be.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscussionThreadRequest {

    private UUID lessonId;

    @NotBlank(message = "Tiêu đề thảo luận không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung thảo luận không được để trống")
    private String content;
}
