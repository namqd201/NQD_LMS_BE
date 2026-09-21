package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("User {} updated their profile", user.getEmail());
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToUserProfileResponse(user);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(new HashSet<>(roles))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
