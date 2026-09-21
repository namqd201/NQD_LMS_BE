package com.nqd.nqd_lms_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success;

    public static MessageResponse of(String message) {
        return MessageResponse.builder()
                .message(message)
                .success(true)
                .build();
    }
}
