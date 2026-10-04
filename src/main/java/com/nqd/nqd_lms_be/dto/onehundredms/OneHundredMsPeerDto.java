package com.nqd.nqd_lms_be.dto.onehundredms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneHundredMsPeerDto {
    private String id;
    private String name;
    private String role;
    private String userId;
    private String joinedAt;
}
