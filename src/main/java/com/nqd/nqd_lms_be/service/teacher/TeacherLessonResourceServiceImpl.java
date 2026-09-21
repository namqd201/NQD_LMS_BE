package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.ReorderItemsRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Lesson;
import com.nqd.nqd_lms_be.entity.LessonResource;
import com.nqd.nqd_lms_be.repository.CourseTeacherRepository;
import com.nqd.nqd_lms_be.repository.LessonRepository;
import com.nqd.nqd_lms_be.repository.LessonResourceRepository;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherLessonResourceServiceImpl implements TeacherLessonResourceService {

    private final LessonResourceRepository lessonResourceRepository;
    private final LessonRepository lessonRepository;
    private final CourseTeacherRepository courseTeacherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResourceResponse> getLessonResources(UUID teacherId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        return lessonResourceRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherResourceResponse addResource(UUID teacherId, UUID lessonId, TeacherResourceRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : lessonResourceRepository.findMaxDisplayOrderByLessonId(lessonId) + 1;

        LessonResource resource = LessonResource.builder()
                .lesson(lesson)
                .resourceType(request.getResourceType())
                .title(request.getTitle())
                .url(request.getUrl())
                .metadata(request.getMetadata())
                .displayOrder(displayOrder)
                .build();

        resource = lessonResourceRepository.save(resource);
        log.info("Teacher {} added resource {} to lesson {}", teacherId, resource.getId(), lessonId);
        return mapToResponse(resource);
    }

    @Override
    @Transactional
    public TeacherResourceResponse updateResource(UUID teacherId, UUID lessonId, UUID resourceId, TeacherResourceRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        LessonResource resource = lessonResourceRepository.findByIdAndLessonId(resourceId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonResource", resourceId));

        resource.setResourceType(request.getResourceType());
        resource.setTitle(request.getTitle());
        resource.setUrl(request.getUrl());
        resource.setMetadata(request.getMetadata());
        if (request.getDisplayOrder() != null) {
            resource.setDisplayOrder(request.getDisplayOrder());
        }

        resource = lessonResourceRepository.save(resource);
        log.info("Teacher {} updated resource {} in lesson {}", teacherId, resourceId, lessonId);
        return mapToResponse(resource);
    }

    @Override
    @Transactional
    public void deleteResource(UUID teacherId, UUID lessonId, UUID resourceId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        LessonResource resource = lessonResourceRepository.findByIdAndLessonId(resourceId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonResource", resourceId));

        lessonResourceRepository.delete(resource);
        log.info("Teacher {} deleted resource {} from lesson {}", teacherId, resourceId, lessonId);
    }

    @Override
    @Transactional
    public void reorderResources(UUID teacherId, UUID lessonId, ReorderItemsRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        List<UUID> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID resId = orderedIds.get(i);
            LessonResource resource = lessonResourceRepository.findByIdAndLessonId(resId, lessonId)
                    .orElse(null);
            if (resource != null) {
                resource.setDisplayOrder(i + 1);
                lessonResourceRepository.save(resource);
            }
        }
        log.info("Teacher {} reordered resources for lesson {}", teacherId, lessonId);
    }

    private void verifyCourseOwnership(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("You do not have permission to manage this lesson's resources");
        }
    }

    private TeacherResourceResponse mapToResponse(LessonResource res) {
        return TeacherResourceResponse.builder()
                .id(res.getId())
                .lessonId(res.getLesson().getId())
                .resourceType(res.getResourceType())
                .title(res.getTitle())
                .url(res.getUrl())
                .metadata(res.getMetadata())
                .displayOrder(res.getDisplayOrder())
                .createdAt(res.getCreatedAt())
                .updatedAt(res.getUpdatedAt())
                .build();
    }
}
