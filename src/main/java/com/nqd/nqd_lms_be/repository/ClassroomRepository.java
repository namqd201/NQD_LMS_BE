package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    List<Classroom> findByTeacherIdAndStatusOrderByCreatedAtDesc(UUID teacherId, ClassroomStatus status);

    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);

    Optional<Classroom> findByCode(String code);

    boolean existsByCode(String code);

    long countByTeacherId(UUID teacherId);

    @Query("SELECT c FROM Classroom c " +
           "JOIN ClassroomStudent cs ON cs.classroom.id = c.id " +
           "WHERE cs.student.id = :studentId AND cs.status = 'ENROLLED' " +
           "ORDER BY cs.joinedAt DESC")
    List<Classroom> findEnrolledClassroomsByStudentId(@Param("studentId") UUID studentId);
}
