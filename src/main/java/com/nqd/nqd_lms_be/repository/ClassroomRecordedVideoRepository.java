package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomRecordedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomRecordedVideoRepository extends JpaRepository<ClassroomRecordedVideo, UUID> {
    List<ClassroomRecordedVideo> findByClassroomIdOrderBySessionDateDescCreatedAtDesc(UUID classroomId);
}
