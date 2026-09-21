package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.StudentCourseDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonDetailResponse;

import java.util.UUID;

public interface StudentCourseStructureService {
    StudentCourseDetailResponse getPublishedCourseDetail(UUID courseId, UUID studentId);
    StudentLessonDetailResponse getPublishedLesson(UUID lessonId, UUID studentId);
}
