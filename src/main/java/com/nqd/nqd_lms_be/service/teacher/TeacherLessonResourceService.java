package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.ReorderItemsRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceResponse;

import java.util.List;
import java.util.UUID;

public interface TeacherLessonResourceService {
    List<TeacherResourceResponse> getLessonResources(UUID teacherId, UUID lessonId);
    TeacherResourceResponse addResource(UUID teacherId, UUID lessonId, TeacherResourceRequest request);
    TeacherResourceResponse updateResource(UUID teacherId, UUID lessonId, UUID resourceId, TeacherResourceRequest request);
    void deleteResource(UUID teacherId, UUID lessonId, UUID resourceId);
    void reorderResources(UUID teacherId, UUID lessonId, ReorderItemsRequest request);
}
