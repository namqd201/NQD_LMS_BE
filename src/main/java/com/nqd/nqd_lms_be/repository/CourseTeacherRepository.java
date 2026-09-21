package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.CourseTeacher;
import com.nqd.nqd_lms_be.entity.CourseTeacherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, CourseTeacherId> {
    List<CourseTeacher> findByCourseId(UUID courseId);
    List<CourseTeacher> findByTeacherId(UUID teacherId);
    boolean existsByCourseIdAndTeacherId(UUID courseId, UUID teacherId);
}
