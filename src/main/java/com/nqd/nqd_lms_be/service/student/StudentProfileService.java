package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;

import java.util.UUID;

public interface StudentProfileService {
    UserProfileResponse getMyProfile(UUID studentId);
    UserProfileResponse updateMyProfile(UUID studentId, UpdateProfileRequest request);
}
