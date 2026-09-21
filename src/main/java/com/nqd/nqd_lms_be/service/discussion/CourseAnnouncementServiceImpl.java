package com.nqd.nqd_lms_be.service.discussion;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.discussion.CourseAnnouncementResponse;
import com.nqd.nqd_lms_be.dto.discussion.CreateCourseAnnouncementRequest;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.CourseAnnouncement;
import com.nqd.nqd_lms_be.entity.CourseEnrollment;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.CourseAnnouncementRepository;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseAnnouncementServiceImpl implements CourseAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseAnnouncementResponse> getCourseAnnouncements(UUID courseId, Pageable pageable) {
        Page<CourseAnnouncement> page = announcementRepository.findByCourseIdAndIsDeletedFalseOrderByPostedAtDesc(courseId, pageable);
        return PageResponse.fromPage(page, CourseAnnouncementResponse::fromEntity);
    }

    @Override
    @Transactional
    public CourseAnnouncementResponse createAnnouncement(UUID courseId, UUID authorId, CreateCourseAnnouncementRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(authorId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ giảng viên phụ trách khóa học hoặc Quản trị viên mới được phép đăng thông báo.");
        }

        CourseAnnouncement announcement = CourseAnnouncement.builder()
                .course(course)
                .author(author)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .postedAt(LocalDateTime.now())
                .build();

        announcement = announcementRepository.save(announcement);
        log.info("Teacher {} created announcement {} for course {}", author.getEmail(), announcement.getId(), courseId);

        // Notify all enrolled students in course
        try {
            List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
            String title = "Thông báo mới từ khóa học: " + course.getName();
            String body = announcement.getTitle();
            String linkUrl = "/courses/" + courseId + "?tab=announcements";

            for (CourseEnrollment ce : enrollments) {
                if (ce.getStudent() != null && !ce.getStudent().getId().equals(authorId)) {
                    kafkaNotificationProducer.sendNotification(ce.getStudent().getId(), "COURSE_ANNOUNCEMENT", title, body, linkUrl);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to broadcast announcement notifications: {}", ex.getMessage());
        }

        return CourseAnnouncementResponse.fromEntity(announcement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(UUID courseId, UUID announcementId, UUID authorId) {
        CourseAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", announcementId));

        Course course = announcement.getCourse();
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(authorId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Bạn không có quyền xóa thông báo này.");
        }

        announcement.setIsDeleted(true);
        announcementRepository.save(announcement);
        log.info("Announcement {} deleted by user {}", announcementId, authorId);
    }
}
