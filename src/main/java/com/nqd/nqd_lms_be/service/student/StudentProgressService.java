package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.StudentCourseProgressResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonProgressResponse;
import com.nqd.nqd_lms_be.dto.student.StudentOverallAnalyticsResponse;
import com.nqd.nqd_lms_be.dto.student.UpdateLessonProgressRequest;

import java.util.UUID;

public interface StudentProgressService {
    StudentLessonProgressResponse getLessonProgress(UUID studentId, UUID lessonId);
    StudentLessonProgressResponse updateLessonProgress(UUID studentId, UUID lessonId, UpdateLessonProgressRequest request);
    StudentCourseProgressResponse getCourseProgress(UUID studentId, UUID courseId);
    StudentOverallAnalyticsResponse getStudentOverallAnalytics(UUID studentId);
}
