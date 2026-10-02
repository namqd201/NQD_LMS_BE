package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.LessonSlide;
import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonSlideRepository extends JpaRepository<LessonSlide, UUID> {

    Optional<LessonSlide> findByTargetTypeAndTargetId(SlideTargetType targetType, UUID targetId);

    void deleteByTargetTypeAndTargetId(SlideTargetType targetType, UUID targetId);
}
