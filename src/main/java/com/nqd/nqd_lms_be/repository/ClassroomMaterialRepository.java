package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomMaterialRepository extends JpaRepository<ClassroomMaterial, UUID> {
    List<ClassroomMaterial> findByClassroomIdOrderByCreatedAtDesc(UUID classroomId);
}
