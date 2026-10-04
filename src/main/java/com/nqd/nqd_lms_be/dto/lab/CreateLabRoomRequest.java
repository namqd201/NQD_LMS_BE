package com.nqd.nqd_lms_be.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabRoomRequest {

    @NotBlank(message = "Tiêu đề phòng Lab không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Tên diễn giả/người chia sẻ không được để trống")
    private String speakerName;

    private String speakerTitle; // "Giáo sư", "Tiến sĩ", "Thạc sĩ", "Giảng viên", "Chuyên gia"...

    private String coverImageUrl;

    @NotNull(message = "Thời gian bắt đầu buổi Lab không được để trống")
    private LocalDateTime scheduledStartTime;

    @Builder.Default
    private Integer estimatedDurationMinutes = 60;
}
