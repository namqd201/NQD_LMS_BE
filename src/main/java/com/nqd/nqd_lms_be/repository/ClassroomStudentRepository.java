package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomStudent;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudent, UUID> {

    List<ClassroomStudent> findByClassroomIdAndStatusOrderByJoinedAtDesc(UUID classroomId, ClassEnrollmentStatus status);

    List<ClassroomStudent> findByClassroomIdOrderByCreatedAtDesc(UUID classroomId);

    Optional<ClassroomStudent> findByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    boolean existsByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    List<ClassroomStudent> findByStudentIdAndStatusOrderByCreatedAtDesc(UUID studentId, ClassEnrollmentStatus status);

    long countByClassroomIdAndStatus(UUID classroomId, ClassEnrollmentStatus status);

    @Query("SELECT COUNT(cs) FROM ClassroomStudent cs " +
           "WHERE cs.classroom.teacher.id = :teacherId AND cs.status = 'PENDING_APPROVAL'")
    long countPendingRequestsForTeacher(@Param("teacherId") UUID teacherId);
}
