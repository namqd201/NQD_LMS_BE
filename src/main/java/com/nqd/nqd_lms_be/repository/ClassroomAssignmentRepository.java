package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomAssignmentRepository extends JpaRepository<ClassroomAssignment, UUID> {
    List<ClassroomAssignment> findByClassroomIdOrderByCreatedAtDesc(UUID classroomId);
}
