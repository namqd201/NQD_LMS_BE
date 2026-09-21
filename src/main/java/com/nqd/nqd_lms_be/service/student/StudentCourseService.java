package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.StudentCourseResponse;

import java.util.List;
import java.util.UUID;

public interface StudentCourseService {
    List<StudentCourseResponse> getPublishedCourses(UUID studentId);
    List<StudentCourseResponse> getEnrolledCourses(UUID studentId);
    void enrollCourse(UUID courseId, UUID studentId);
}
