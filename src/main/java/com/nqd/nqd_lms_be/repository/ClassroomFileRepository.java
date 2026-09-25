package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomFileRepository extends JpaRepository<ClassroomFile, UUID> {
    List<ClassroomFile> findByClassroomIdOrderByCreatedAtDesc(UUID classroomId);
}
