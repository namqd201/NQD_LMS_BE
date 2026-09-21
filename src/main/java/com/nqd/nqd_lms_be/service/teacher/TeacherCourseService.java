package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.CourseAnalyticsResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherEnrollmentResponse;

import java.util.List;
import java.util.UUID;

public interface TeacherCourseService {
    List<TeacherCourseResponse> getTeacherCourses(UUID teacherId);
    TeacherCourseResponse getCourseById(UUID courseId, UUID teacherId);
    TeacherCourseResponse createCourse(TeacherCourseRequest request, UUID teacherId);
    TeacherCourseResponse updateCourse(UUID courseId, TeacherCourseRequest request, UUID teacherId);
    void deleteCourse(UUID courseId, UUID teacherId);
    List<TeacherCourseResponse> getDeletedCourses(UUID teacherId);
    TeacherCourseResponse restoreCourse(UUID courseId, UUID teacherId);
    CourseAnalyticsResponse getCourseAnalytics(UUID courseId, UUID teacherId);
    List<TeacherEnrollmentResponse> getCourseEnrollments(UUID courseId, UUID teacherId);
    void approveEnrollment(UUID courseId, UUID enrollmentId, UUID teacherId);
    void rejectEnrollment(UUID courseId, UUID enrollmentId, UUID teacherId);
}
