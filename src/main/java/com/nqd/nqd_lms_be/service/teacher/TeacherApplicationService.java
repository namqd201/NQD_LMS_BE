package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.RejectTeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationResponse;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface TeacherApplicationService {
    String uploadVerificationDocument(UUID userId, MultipartFile file);
    TeacherApplicationResponse submitApplication(UUID userId, TeacherApplicationRequest request);
    TeacherApplicationResponse getMyApplication(UUID userId);
    TeacherApplicationResponse updateMyApplication(UUID userId, TeacherApplicationRequest request);

    // Admin operations
    Page<TeacherApplicationResponse> getApplications(TeacherApplicationStatus status, TeacherApplicantType applicantType, String keyword, Pageable pageable);
    TeacherApplicationResponse getApplicationById(UUID id);
    TeacherApplicationResponse approveApplication(UUID adminId, UUID applicationId);
    TeacherApplicationResponse rejectApplication(UUID adminId, UUID applicationId, RejectTeacherApplicationRequest request);
    long countPendingApplications();
}
