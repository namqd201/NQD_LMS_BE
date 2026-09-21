package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);
    void deleteByCourseId(UUID courseId);
    long countByCourseId(UUID courseId);
}
