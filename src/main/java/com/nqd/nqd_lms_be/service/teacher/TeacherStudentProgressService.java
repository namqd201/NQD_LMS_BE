package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.TeacherStudentDetailProgressResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherStudentLessonProgressResponse;

import java.util.List;
import java.util.UUID;

public interface TeacherStudentProgressService {
    List<TeacherStudentLessonProgressResponse> getCourseStudentProgress(UUID teacherId, UUID courseId);
    List<TeacherStudentLessonProgressResponse> getLessonStudentProgress(UUID teacherId, UUID lessonId);
    TeacherStudentDetailProgressResponse getStudentProgressDetail(UUID teacherId, UUID studentId);
    void unlockLesson(UUID teacherId, UUID courseId, UUID studentId, UUID lessonId);
    void unlockAllLessons(UUID teacherId, UUID courseId, UUID studentId);
}
