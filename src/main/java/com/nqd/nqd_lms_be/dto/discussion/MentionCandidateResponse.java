package com.nqd.nqd_lms_be.dto.discussion;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentionCandidateResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roleInCourse; // "TEACHER" or "STUDENT"
}
