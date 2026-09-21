package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.StudentResourceResponse;

import java.util.List;
import java.util.UUID;

public interface StudentLessonResourceService {
    List<StudentResourceResponse> getLessonResources(UUID studentId, UUID lessonId);
}
