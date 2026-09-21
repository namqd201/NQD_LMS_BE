package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.TeacherBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherBankAccountRepository extends JpaRepository<TeacherBankAccount, UUID> {

    List<TeacherBankAccount> findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID teacherId);

    Optional<TeacherBankAccount> findByTeacherIdAndIsDefaultTrueAndIsDeletedFalse(UUID teacherId);

    Optional<TeacherBankAccount> findByIdAndTeacherIdAndIsDeletedFalse(UUID id, UUID teacherId);
}
