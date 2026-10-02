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

    Optional<Classroom> findFirstByMeetingId(String meetingId);

    Optional<Classroom> findFirstByLarkMeetingUrlContaining(String meetingNo);

    long countByStatus(ClassroomStatus status);

    long countByIsLiveNowTrue();

    @Query("SELECT COALESCE(SUM(c.studentCount), 0) FROM Classroom c")
    long sumAllStudentCount();

    @Query("SELECT c FROM Classroom c " +
           "WHERE (:status IS NULL OR c.status = :status) " +
           "AND (:query IS NULL OR :query = '' " +
           "     OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(c.teacher.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(c.teacher.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<Classroom> searchAllForAdmin(@Param("query") String query, @Param("status") ClassroomStatus status);
}
