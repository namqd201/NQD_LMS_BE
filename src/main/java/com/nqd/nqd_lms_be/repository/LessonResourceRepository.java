package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {

    List<LessonResource> findByLessonIdOrderByDisplayOrderAsc(UUID lessonId);

    Optional<LessonResource> findByIdAndLessonId(UUID id, UUID lessonId);

    @Query("SELECT COALESCE(MAX(r.displayOrder), 0) FROM LessonResource r WHERE r.lesson.id = :lessonId")
    Integer findMaxDisplayOrderByLessonId(@Param("lessonId") UUID lessonId);
}
