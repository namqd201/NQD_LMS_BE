package com.nqd.nqd_lms_be.dto.lab;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabVideoRequest {
    @NotBlank(message = "Tiêu đề video không được để trống")
    private String title;

    @NotBlank(message = "Đường dẫn video không được để trống")
    private String videoUrl;

    private Integer durationMinutes;
    private LocalDate recordedDate;
    private String description;
}
