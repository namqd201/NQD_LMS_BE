package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;

import java.util.UUID;

public interface UserService {
    UserProfileResponse getCurrentUserProfile(UUID userId);
    UserProfileResponse updateCurrentUserProfile(UUID userId, UpdateProfileRequest request);
    UserProfileResponse getUserProfileById(UUID userId);
}
