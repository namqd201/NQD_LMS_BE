package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.admin.SubjectRequest;
import com.nqd.nqd_lms_be.dto.admin.SubjectResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectServiceImpl implements AdminSubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
        if (Boolean.TRUE.equals(subject.getIsDeleted())) {
            throw new ResourceNotFoundException("Subject", id);
        }
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = Subject.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : SubjectStatus.ACTIVE)
                .build();

        subject = subjectRepository.save(subject);
        log.info("Admin created subject: {} ({})", subject.getName(), subject.getCode());
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setName(request.getName());
        subject.setCode(request.getCode().toUpperCase());
        subject.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            subject.setStatus(request.getStatus());
        }

        subject = subjectRepository.save(subject);
        log.info("Admin updated subject: {}", id);
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubjectStatus(UUID id, SubjectStatus status) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setStatus(status);
        subject = subjectRepository.save(subject);
        log.info("Admin updated subject {} status to {}", id, status);
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setIsDeleted(true);
        subject.setDeletedAt(java.time.LocalDateTime.now());
        subjectRepository.save(subject);
        log.info("Admin soft-deleted subject: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getDeletedSubjects() {
        return subjectRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsDeleted()))
                .sorted((a, b) -> {
                    java.time.LocalDateTime tA = a.getDeletedAt() != null ? a.getDeletedAt() : a.getCreatedAt();
                    java.time.LocalDateTime tB = b.getDeletedAt() != null ? b.getDeletedAt() : b.getCreatedAt();
                    if (tA == null && tB == null) return 0;
                    if (tA == null) return 1;
                    if (tB == null) return -1;
                    return tB.compareTo(tA);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectResponse restoreSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setIsDeleted(false);
        subject.setDeletedAt(null);
        subject.setDeletedBy(null);
        subject = subjectRepository.save(subject);
        log.info("Admin restored subject: {}", id);
        return mapToResponse(subject);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        long count = courseRepository.countBySubjectId(subject.getId());
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .status(subject.getStatus())
                .courseCount(count)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .isDeleted(subject.getIsDeleted())
                .deletedAt(subject.getDeletedAt())
                .deletedBy(subject.getDeletedBy())
                .build();
    }
}
