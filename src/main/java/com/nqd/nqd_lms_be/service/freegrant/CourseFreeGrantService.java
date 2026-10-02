package com.nqd.nqd_lms_be.service.freegrant;

import com.nqd.nqd_lms_be.dto.freegrant.*;
import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseFreeGrantService {

    CourseFreeGrantSummaryResponse getFreeGrantSummary(UUID courseId, UUID teacherId);

    TeacherFreeGrantResponse addFreeGrant(UUID courseId, UUID teacherId, AddFreeGrantRequest request);

    CourseFreeQuotaRequestResponse requestQuota(UUID courseId, UUID teacherId, CreateQuotaRequest request);

    Page<CourseFreeQuotaRequestResponse> getAdminQuotaRequests(FreeQuotaRequestStatus status, Pageable pageable);

    CourseFreeQuotaRequestResponse reviewQuotaRequest(UUID requestId, ReviewQuotaRequest request, String adminEmail);
}
