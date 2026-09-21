package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.StudentResourceResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Lesson;
import com.nqd.nqd_lms_be.entity.LessonResource;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.EntitlementRepository;
import com.nqd.nqd_lms_be.repository.LessonRepository;
import com.nqd.nqd_lms_be.repository.LessonResourceRepository;
import com.nqd.nqd_lms_be.util.SecurityUtils;
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
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentLessonResourceServiceImpl implements StudentLessonResourceService {

    private final LessonResourceRepository lessonResourceRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EntitlementRepository entitlementRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public List<StudentResourceResponse> getLessonResources(UUID studentId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        Course course = lesson.getChapter().getCourse();

        // 1. Check Course is published
        if (!course.isPublished()) {
            throw new ForbiddenOperationException("Cannot access resources for unpublished course");
        }

        // 2. Check Lesson is PUBLISHED
        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new ForbiddenOperationException("Cannot access resources for unpublished lesson");
        }

        boolean isOwner = course.getCreator() != null && course.getCreator().getId().equals(studentId);
        boolean isEnrolled = isOwner || courseEnrollmentRepository.existsByCourseIdAndStudentId(course.getId(), studentId) ||
                entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                        studentId, EntitlementType.COURSE_ACCESS, course.getId(), EntitlementStatus.ACTIVE
                ).isPresent();

        boolean isUltra = studentId != null && membershipEntitlementService.hasFeature(studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.ULTRA_UNLIMITED_COURSES);
        boolean hasChapterAccess = studentId != null && entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                studentId, EntitlementType.CHAPTER_ACCESS, lesson.getChapter().getId(), EntitlementStatus.ACTIVE
        ).isPresent();
        boolean hasLessonAccess = studentId != null && entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                studentId, EntitlementType.LESSON_ACCESS, lesson.getId(), EntitlementStatus.ACTIVE
        ).isPresent();

        boolean hasAccess = isEnrolled || isUltra || hasChapterAccess || hasLessonAccess;

        List<LessonResource> resources = lessonResourceRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId);

        // If paid course and student has no paid access and not admin: only return preview resources
        if (course.isPaid() && !hasAccess && !SecurityUtils.isAdmin()) {
            if (!Boolean.TRUE.equals(lesson.getIsPreview())) {
                throw new ForbiddenOperationException("Vui lòng mua khóa học, chương hoặc bài học để truy cập tài liệu bài học này.");
            }
            // For preview lesson, only return resources where isPreview is true or all resources of preview lesson if permitted
            resources = resources.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsPreview()) || Boolean.TRUE.equals(lesson.getIsPreview()))
                    .collect(Collectors.toList());
        }

        return resources.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private StudentResourceResponse mapToResponse(LessonResource res) {
        return StudentResourceResponse.builder()
                .id(res.getId())
                .lessonId(res.getLesson().getId())
                .resourceType(res.getResourceType())
                .title(res.getTitle())
                .url(res.getUrl())
                .metadata(res.getMetadata())
                .displayOrder(res.getDisplayOrder())
                .isPreview(Boolean.TRUE.equals(res.getIsPreview()))
                .build();
    }
}
