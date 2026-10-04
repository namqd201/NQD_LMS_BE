package com.nqd.nqd_lms_be.dto.classroom;

import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsPeerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomLivePresenceDto {
    private UUID classroomId;
    private String roomId;
    private boolean isLiveNow;
    private boolean hostOnline;
    private int participantCount;
    @Builder.Default
    private List<OneHundredMsPeerDto> peers = new ArrayList<>();
}
