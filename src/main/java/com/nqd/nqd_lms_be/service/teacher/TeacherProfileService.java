package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;

import java.util.UUID;

public interface TeacherProfileService {
    UserProfileResponse getMyProfile(UUID teacherId);
    UserProfileResponse updateMyProfile(UUID teacherId, UpdateProfileRequest request);
}
