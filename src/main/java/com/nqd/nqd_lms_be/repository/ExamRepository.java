package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Exam;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID>, JpaSpecificationExecutor<Exam> {
    List<Exam> findByCreatorId(UUID creatorId);
    List<Exam> findByCourseId(UUID courseId);
    List<Exam> findByStatus(ExamStatus status);
    List<Exam> findByCreatorIdAndIsDeletedFalse(UUID creatorId);
    List<Exam> findByCourseIdAndIsDeletedFalse(UUID courseId);
    List<Exam> findByStatusAndIsDeletedFalse(ExamStatus status);
    boolean existsByCode(String code);
}
