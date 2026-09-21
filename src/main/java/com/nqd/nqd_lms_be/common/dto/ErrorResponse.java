package com.nqd.nqd_lms_be.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String message;

    private String errorCode;

    private int status;

    private String path;

    private Map<String, String> errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ErrorResponse of(String message, String errorCode, int status) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .build();
    }

    public static ErrorResponse of(String message, String errorCode, int status, Map<String, String> errors) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .errors(errors)
                .build();
    }
}
