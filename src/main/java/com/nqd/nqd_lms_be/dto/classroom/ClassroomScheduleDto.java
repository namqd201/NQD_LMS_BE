package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

public class ClassroomScheduleDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Ngày trong tuần không được để trống")
        private String dayOfWeek; // MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
        @NotBlank(message = "Giờ bắt đầu không được để trống")
        private String startTime; // e.g. "19:30"
        @NotBlank(message = "Giờ kết thúc không được để trống")
        private String endTime; // e.g. "21:00"
        @NotBlank(message = "Tên buổi học không được để trống")
        private String title;
        private String roomNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID classroomId;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String title;
        private String roomNote;
    }
}
