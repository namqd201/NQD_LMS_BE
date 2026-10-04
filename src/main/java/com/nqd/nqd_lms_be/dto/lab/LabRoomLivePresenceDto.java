package com.nqd.nqd_lms_be.dto.lab;

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
public class LabRoomLivePresenceDto {
    private UUID labId;
    private String status;
    private boolean isLiveNow;
    private int participantCount;
    @Builder.Default
    private List<OneHundredMsPeerDto> peers = new ArrayList<>();
}
