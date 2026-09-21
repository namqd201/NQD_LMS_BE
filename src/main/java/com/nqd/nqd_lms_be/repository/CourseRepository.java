package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {
    Optional<Course> findByCode(String code);

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByStatusAndIsDeletedFalse(CourseStatus status);

    List<Course> findByStatusInAndIsDeletedFalse(Collection<CourseStatus> statuses);

    Page<Course> findByStatusAndIsDeletedFalse(CourseStatus status, Pageable pageable);

    Page<Course> findByStatusInAndIsDeletedFalse(Collection<CourseStatus> statuses, Pageable pageable);

    List<Course> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT c FROM Course c WHERE (c.isDeleted = false OR c.isDeleted IS NULL) AND (c.creator.id = :teacherId OR c.id IN (SELECT ct.courseId FROM CourseTeacher ct WHERE ct.teacherId = :teacherId)) ORDER BY c.createdAt DESC")
    List<Course> findCoursesByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT COUNT(c) > 0 FROM Course c WHERE c.id = :courseId AND (c.creator.id = :teacherId OR c.id IN (SELECT ct.courseId FROM CourseTeacher ct WHERE ct.teacherId = :teacherId))")
    boolean isTeacherOwnerOrAssigned(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);

    long countBySubjectId(UUID subjectId);

    long countByCreatorIdAndIsDeletedFalse(UUID creatorId);

    List<Course> findBySubjectId(UUID subjectId);

    List<Course> findBySubjectIdAndIsDeletedFalse(UUID subjectId);
}
