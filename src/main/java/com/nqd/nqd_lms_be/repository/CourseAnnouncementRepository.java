package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {

    @Query(value = "SELECT a FROM CourseAnnouncement a LEFT JOIN FETCH a.author WHERE a.course.id = :courseId AND a.isDeleted = false ORDER BY a.postedAt DESC",
           countQuery = "SELECT COUNT(a) FROM CourseAnnouncement a WHERE a.course.id = :courseId AND a.isDeleted = false")
    Page<CourseAnnouncement> findByCourseIdAndIsDeletedFalseOrderByPostedAtDesc(@Param("courseId") UUID courseId, Pageable pageable);
}
