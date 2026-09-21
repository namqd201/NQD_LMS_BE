package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, UUID> {

    List<ExamAssignment> findByExamId(UUID examId);

    List<ExamAssignment> findByStudentId(UUID studentId);

    Optional<ExamAssignment> findByExamIdAndStudentId(UUID examId, UUID studentId);

    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);

    @Modifying
    @Query("DELETE FROM ExamAssignment ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId")
    void deleteByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);

    @Modifying
    @Query("DELETE FROM ExamAssignment ea WHERE ea.exam.id = :examId")
    void deleteByExamId(@Param("examId") UUID examId);

    long countByExamId(UUID examId);
}
