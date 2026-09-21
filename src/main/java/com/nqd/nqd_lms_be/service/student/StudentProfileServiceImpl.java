package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentProfileServiceImpl implements StudentProfileService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UUID studentId, UpdateProfileRequest request) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("Student {} updated profile", user.getEmail());
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
