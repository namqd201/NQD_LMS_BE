package com.nqd.nqd_lms_be.dto.discussion;

import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactDiscussionRequest {
    private PostReactionType type;
}
