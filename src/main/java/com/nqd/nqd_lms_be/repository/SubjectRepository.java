package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByCode(String code);
    java.util.List<Subject> findByStatus(com.nqd.nqd_lms_be.entity.enums.SubjectStatus status);
    java.util.List<Subject> findByStatusAndIsDeletedFalse(com.nqd.nqd_lms_be.entity.enums.SubjectStatus status);
    java.util.List<Subject> findByIsDeletedFalseOrderByCreatedAtDesc();
}
