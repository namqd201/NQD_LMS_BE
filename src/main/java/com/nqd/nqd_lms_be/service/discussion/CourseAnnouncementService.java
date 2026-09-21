package com.nqd.nqd_lms_be.service.discussion;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.dto.discussion.CourseAnnouncementResponse;
import com.nqd.nqd_lms_be.dto.discussion.CreateCourseAnnouncementRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseAnnouncementService {

    PageResponse<CourseAnnouncementResponse> getCourseAnnouncements(UUID courseId, Pageable pageable);

    CourseAnnouncementResponse createAnnouncement(UUID courseId, UUID authorId, CreateCourseAnnouncementRequest request);

    void deleteAnnouncement(UUID courseId, UUID announcementId, UUID authorId);
}
