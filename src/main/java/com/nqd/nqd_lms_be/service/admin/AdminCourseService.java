package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminCourseDisableRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminCourseResponse;

import java.util.List;
import java.util.UUID;

public interface AdminCourseService {
    List<AdminCourseResponse> getAllCourses();
    AdminCourseResponse getCourseById(UUID id);
    AdminCourseResponse disableCourse(UUID id, AdminCourseDisableRequest request, UUID adminId);
    AdminCourseResponse enableCourse(UUID id, UUID adminId);
    void deleteCourse(UUID id, UUID adminId);
}
