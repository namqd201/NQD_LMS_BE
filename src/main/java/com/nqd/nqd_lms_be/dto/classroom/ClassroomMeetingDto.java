package com.nqd.nqd_lms_be.dto.classroom;

import lombok.*;

import java.util.UUID;

public class ClassroomMeetingDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String larkMeetingUrl;
        private String meetingId;
        private String passcode;
        private String meetingNote;
        private Boolean isLiveNow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID classroomId;
        private String larkMeetingUrl;
        private String meetingId;
        private String passcode;
        private String meetingNote;
        private Boolean isLiveNow;
    }
}
