package com.nqd.nqd_lms_be.service.course;

import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;

import java.util.UUID;

public interface CourseWorkflowService {
    TeacherCourseResponse submitForReview(UUID courseId, UUID teacherId);
    TeacherCourseResponse approveCourse(UUID courseId, UUID adminId);
    TeacherCourseResponse rejectCourse(UUID courseId, UUID adminId, String reason);
}
