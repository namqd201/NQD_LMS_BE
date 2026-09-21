package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.dto.admin.SubjectRequest;
import com.nqd.nqd_lms_be.dto.admin.SubjectResponse;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;

import java.util.List;
import java.util.UUID;

public interface AdminSubjectService {
    List<SubjectResponse> getAllSubjects();
    SubjectResponse getSubjectById(UUID id);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(UUID id, SubjectRequest request);
    SubjectResponse updateSubjectStatus(UUID id, SubjectStatus status);
    void deleteSubject(UUID id);
    List<SubjectResponse> getDeletedSubjects();
    SubjectResponse restoreSubject(UUID id);
}
