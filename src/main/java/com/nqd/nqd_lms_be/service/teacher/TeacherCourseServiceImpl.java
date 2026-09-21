package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.CourseAnalyticsResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherEnrollmentResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.CourseEnrollment;
import com.nqd.nqd_lms_be.entity.CourseTeacher;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherCourseServiceImpl implements TeacherCourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ExamRepository examRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final LessonRepository lessonRepository;
    private final com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer kafkaNotificationProducer;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherCourseResponse> getTeacherCourses(UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return courseRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc().stream()
                    .map(this::mapToCourseResponse)
                    .collect(Collectors.toList());
        }
        return courseRepository.findCoursesByTeacherId(teacherId).stream()
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherCourseResponse getCourseById(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (Boolean.TRUE.equals(course.getIsDeleted())) {
            throw new ResourceNotFoundException("Course", courseId);
        }

        verifyCourseOwnershipOrTeaching(course, teacherId);
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public TeacherCourseResponse createCourse(TeacherCourseRequest request, UUID teacherId) {
        // Enforce Class/Course limit for Free Teacher
        membershipEntitlementService.enforceAndConsumeUsage(
                teacherId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.CLASS_LIMIT, 1
        );

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        User creator = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        String courseCode = request.getCode() != null ? request.getCode().trim().toUpperCase() : "";
        if (courseCode.isEmpty()) {
            String prefix = subject.getCode() != null ? subject.getCode().toUpperCase() : "COURSE";
            courseCode = prefix + (1000 + (int)(Math.random() * 9000));
        }

        while (courseRepository.findByCode(courseCode).isPresent()) {
            String prefix = subject.getCode() != null ? subject.getCode().toUpperCase() : "COURSE";
            courseCode = prefix + (1000 + (int)(Math.random() * 9000));
        }

        Course course = Course.builder()
                .subject(subject)
                .name(request.getName())
                .code(courseCode)
                .description(request.getDescription())
                .gradeLevel(request.getGradeLevel())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus() != null ? request.getStatus() : CourseStatus.DRAFT)
                .isPrivate(Boolean.TRUE.equals(request.getIsPrivate()))
                .pricingType(request.getPricingType() != null ? request.getPricingType() : com.nqd.nqd_lms_be.entity.enums.CoursePricingType.FREE)
                .price(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO)
                .salePrice(request.getSalePrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .creator(creator)
                .build();

        course = courseRepository.save(course);

        // Also add creator to CourseTeacher table
        CourseTeacher courseTeacher = CourseTeacher.builder()
                .courseId(course.getId())
                .teacherId(teacherId)
                .course(course)
                .teacher(creator)
                .build();
        courseTeacherRepository.save(courseTeacher);

        log.info("Teacher {} created course: {}", creator.getEmail(), course.getName());
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public TeacherCourseResponse updateCourse(UUID courseId, TeacherCourseRequest request, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        if (request.getSubjectId() != null && !request.getSubjectId().equals(course.getSubject().getId())) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));
            course.setSubject(subject);
        }

        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setGradeLevel(request.getGradeLevel());
        course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        if (request.getIsPrivate() != null) {
            course.setIsPrivate(request.getIsPrivate());
        }
        if (request.getPricingType() != null) {
            course.setPricingType(request.getPricingType());
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice());
        }
        if (request.getSalePrice() != null) {
            course.setSalePrice(request.getSalePrice());
        }
        if (request.getCurrency() != null) {
            course.setCurrency(request.getCurrency());
        }

        course = courseRepository.save(course);
        log.info("Course {} updated by teacher {}", courseId, teacherId);
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // Rule: Only the course creator or Admin can delete the course
        if (!SecurityUtils.isAdmin() && (course.getCreator() == null || !course.getCreator().getId().equals(teacherId))) {
            throw new ForbiddenOperationException("Bạn không thể xóa khóa học do người khác tạo. Chỉ người tạo mới có quyền xóa.");
        }

        User user = userRepository.findById(teacherId).orElse(null);
        String userEmail = user != null ? user.getEmail() : teacherId.toString();

        course.setIsDeleted(true);
        course.setDeletedAt(LocalDateTime.now());
        course.setDeletedBy(userEmail);
        courseRepository.save(course);

        log.info("Course {} soft-deleted by user {}", courseId, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherCourseResponse> getDeletedCourses(UUID teacherId) {
        List<Course> allCourses = courseRepository.findAll();
        return allCourses.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDeleted()))
                .filter(c -> SecurityUtils.isAdmin() || (c.getCreator() != null && c.getCreator().getId().equals(teacherId)))
                .sorted((a, b) -> {
                    LocalDateTime tA = a.getDeletedAt() != null ? a.getDeletedAt() : a.getCreatedAt();
                    LocalDateTime tB = b.getDeletedAt() != null ? b.getDeletedAt() : b.getCreatedAt();
                    if (tA == null && tB == null) return 0;
                    if (tA == null) return 1;
                    if (tB == null) return -1;
                    return tB.compareTo(tA);
                })
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherCourseResponse restoreCourse(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        course.setIsDeleted(false);
        course.setDeletedAt(null);
        course.setDeletedBy(null);
        course = courseRepository.save(course);

        log.info("Course {} restored by teacher {}", courseId, teacherId);
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAnalyticsResponse getCourseAnalytics(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        // 1. Enrollment status counts
        List<Object[]> statusCounts = courseEnrollmentRepository.countEnrollmentsByStatusForCourse(courseId);
        long activeCount = 0;
        long completedCount = 0;
        long droppedCount = 0;
        long totalEnrolled = 0;

        for (Object[] row : statusCounts) {
            EnrollmentStatus status = (EnrollmentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalEnrolled += count;
            if (status == EnrollmentStatus.ENROLLED) {
                activeCount += count;
            } else if (status == EnrollmentStatus.COMPLETED) {
                completedCount += count;
            } else if (status == EnrollmentStatus.DROPPED || status == EnrollmentStatus.REJECTED) {
                droppedCount += count;
            }
        }

        double completionRate = totalEnrolled > 0
                ? Math.round((completedCount * 100.0 / totalEnrolled) * 10.0) / 10.0
                : 0.0;

        // 2. Course Progress & Distribution
        long totalLessons = lessonRepository.countByCourseId(courseId);
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseIdWithStudent(courseId);
        List<Object[]> completedLessonsByStudent = lessonProgressRepository.countCompletedLessonsByStudentForCourse(courseId);
        java.util.Map<UUID, Long> studentCompletedMap = new java.util.HashMap<>();
        java.util.Map<UUID, LocalDateTime> studentLastActiveMap = new java.util.HashMap<>();
        for (Object[] row : completedLessonsByStudent) {
            UUID sid = (UUID) row[0];
            long cCount = ((Number) row[1]).longValue();
            LocalDateTime lastActive = row[2] != null ? (LocalDateTime) row[2] : null;
            studentCompletedMap.put(sid, cCount);
            if (lastActive != null) {
                studentLastActiveMap.put(sid, lastActive);
            }
        }

        // Exam scores per student
        List<Object[]> avgScoresByStudent = examAttemptRepository.findAverageScoreByStudentForCourse(courseId);
        java.util.Map<UUID, Double> studentAvgScoreMap = new java.util.HashMap<>();
        for (Object[] row : avgScoresByStudent) {
            UUID sid = (UUID) row[0];
            Double score = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            if (score != null) {
                studentAvgScoreMap.put(sid, Math.round(score * 10.0) / 10.0);
            }
        }

        long r0_25 = 0, r25_50 = 0, r50_75 = 0, r75_100 = 0;
        double sumProgress = 0.0;
        List<CourseAnalyticsResponse.TopStudentSummaryDto> studentSummaries = new java.util.ArrayList<>();

        for (CourseEnrollment ce : enrollments) {
            User student = ce.getStudent();
            long cCount = studentCompletedMap.getOrDefault(student.getId(), 0L);
            double progressPercent = totalLessons > 0
                    ? Math.min(100.0, Math.round((cCount * 100.0 / totalLessons) * 10.0) / 10.0)
                    : (ce.getStatus() == EnrollmentStatus.COMPLETED ? 100.0 : 0.0);

            sumProgress += progressPercent;
            if (progressPercent < 25.0) {
                r0_25++;
            } else if (progressPercent < 50.0) {
                r25_50++;
            } else if (progressPercent < 75.0) {
                r50_75++;
            } else {
                r75_100++;
            }

            LocalDateTime lastActive = studentLastActiveMap.get(student.getId());
            studentSummaries.add(CourseAnalyticsResponse.TopStudentSummaryDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getFullName())
                    .studentEmail(student.getEmail())
                    .avatarUrl(student.getAvatarUrl())
                    .progressPercent(progressPercent)
                    .averageExamScore(studentAvgScoreMap.get(student.getId()))
                    .enrollmentStatus(ce.getStatus().name())
                    .lastAccessedAt(lastActive != null ? lastActive.toString() : (ce.getEnrolledAt() != null ? ce.getEnrolledAt().toString() : null))
                    .build());
        }

        double avgProgress = enrollments.isEmpty() ? 0.0 : Math.round((sumProgress / enrollments.size()) * 10.0) / 10.0;

        // Sort students: top progress first
        studentSummaries.sort((a, b) -> {
            int cmp = Double.compare(b.getProgressPercent() != null ? b.getProgressPercent() : 0.0,
                    a.getProgressPercent() != null ? a.getProgressPercent() : 0.0);
            if (cmp != 0) return cmp;
            return a.getStudentName().compareToIgnoreCase(b.getStudentName());
        });

        // 3. Exam Stats
        long totalExams = examRepository.findByCourseId(courseId).size();
        List<Object[]> examStats = examAttemptRepository.findExamStatsByCourseId(courseId);
        long totalAttempts = 0;
        Double avgExamScore = 0.0;
        long passedAttempts = 0;
        long failedAttempts = 0;

        if (!examStats.isEmpty() && examStats.get(0)[0] != null) {
            Object[] es = examStats.get(0);
            totalAttempts = ((Number) es[0]).longValue();
            avgExamScore = es[1] != null ? Math.round(((Number) es[1]).doubleValue() * 10.0) / 10.0 : 0.0;
            passedAttempts = es[2] != null ? ((Number) es[2]).longValue() : 0;
            failedAttempts = es[3] != null ? ((Number) es[3]).longValue() : 0;
        }

        double passRate = totalAttempts > 0
                ? Math.round((passedAttempts * 100.0 / totalAttempts) * 10.0) / 10.0
                : 0.0;

        // Exam score distribution
        List<Object[]> scoreDistList = examAttemptRepository.findScoreDistributionByCourseId(courseId);
        long below5 = 0, s5_7 = 0, s7_85 = 0, s85_10 = 0;
        if (!scoreDistList.isEmpty() && scoreDistList.get(0) != null) {
            Object[] sd = scoreDistList.get(0);
            below5 = sd[0] != null ? ((Number) sd[0]).longValue() : 0;
            s5_7 = sd[1] != null ? ((Number) sd[1]).longValue() : 0;
            s7_85 = sd[2] != null ? ((Number) sd[2]).longValue() : 0;
            s85_10 = sd[3] != null ? ((Number) sd[3]).longValue() : 0;
        }

        // 4. Drop-off lessons
        List<Object[]> dropOffData = lessonProgressRepository.findLessonDropOffStatsByCourseId(courseId);
        List<CourseAnalyticsResponse.LessonDropOffDto> dropOffLessons = new java.util.ArrayList<>();
        for (Object[] row : dropOffData) {
            UUID lId = (UUID) row[0];
            String lTitle = (String) row[1];
            String chTitle = (String) row[2];
            Integer order = (Integer) row[3];
            long inProgressCount = ((Number) row[4]).longValue();
            long completedLessonCount = ((Number) row[5]).longValue();
            long totalStudentsReached = inProgressCount + completedLessonCount;
            double dropRate = totalStudentsReached > 0
                    ? Math.round((inProgressCount * 100.0 / totalStudentsReached) * 10.0) / 10.0
                    : 0.0;

            if (inProgressCount > 0 || dropOffLessons.size() < 5) {
                dropOffLessons.add(CourseAnalyticsResponse.LessonDropOffDto.builder()
                        .lessonId(lId)
                        .title(lTitle)
                        .chapterTitle(chTitle)
                        .displayOrder(order)
                        .inProgressStudentsCount(inProgressCount)
                        .completedStudentsCount(completedLessonCount)
                        .dropOffRate(dropRate)
                        .build());
            }
        }

        // 5. Daily Activity Timeline (Last 14 days)
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        List<Object[]> dailyCompletions = lessonProgressRepository.findDailyLessonCompletionsByCourseId(courseId, fourteenDaysAgo);
        List<Object[]> dailyExams = examAttemptRepository.findDailyExamSubmissionsByCourseId(courseId, fourteenDaysAgo);

        java.util.Map<String, CourseAnalyticsResponse.DailyActivityDto> activityMap = new java.util.TreeMap<>();
        // Pre-fill last 14 days
        for (int i = 13; i >= 0; i--) {
            String dStr = java.time.LocalDate.now().minusDays(i).toString();
            activityMap.put(dStr, CourseAnalyticsResponse.DailyActivityDto.builder()
                    .date(dStr)
                    .completedLessons(0)
                    .examSubmissions(0)
                    .build());
        }

        for (Object[] row : dailyCompletions) {
            if (row[0] != null) {
                String dStr = row[0].toString();
                long count = ((Number) row[1]).longValue();
                CourseAnalyticsResponse.DailyActivityDto dto = activityMap.computeIfAbsent(dStr, d -> CourseAnalyticsResponse.DailyActivityDto.builder().date(d).completedLessons(0).examSubmissions(0).build());
                dto.setCompletedLessons(count);
            }
        }

        for (Object[] row : dailyExams) {
            if (row[0] != null) {
                String dStr = row[0].toString();
                long count = ((Number) row[1]).longValue();
                CourseAnalyticsResponse.DailyActivityDto dto = activityMap.computeIfAbsent(dStr, d -> CourseAnalyticsResponse.DailyActivityDto.builder().date(d).completedLessons(0).examSubmissions(0).build());
                dto.setExamSubmissions(count);
            }
        }

        return CourseAnalyticsResponse.builder()
                .courseId(courseId)
                .courseName(course.getName())
                .totalEnrolledStudents(totalEnrolled)
                .activeStudents(activeCount)
                .completedStudents(completedCount)
                .droppedStudents(droppedCount)
                .completionRate(completionRate)
                .averageProgressPercent(avgProgress)
                .progressDistribution(CourseAnalyticsResponse.ProgressDistributionDto.builder()
                        .range0To25(r0_25)
                        .range25To50(r25_50)
                        .range50To75(r50_75)
                        .range75To100(r75_100)
                        .build())
                .totalExams(totalExams)
                .totalAttempts(totalAttempts)
                .averageScore(avgExamScore)
                .passRate(passRate)
                .passedAttempts(passedAttempts)
                .failedAttempts(failedAttempts)
                .scoreDistribution(CourseAnalyticsResponse.ScoreDistributionDto.builder()
                        .below5(below5)
                        .range5To7(s5_7)
                        .range7To85(s7_85)
                        .range85To10(s85_10)
                        .build())
                .dropOffLessons(dropOffLessons)
                .activityTimeline(new java.util.ArrayList<>(activityMap.values()))
                .topStudents(studentSummaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherEnrollmentResponse> getCourseEnrollments(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(courseId);
        return enrollments.stream().map(e -> TeacherEnrollmentResponse.builder()
                .enrollmentId(e.getId())
                .courseId(course.getId())
                .courseName(course.getName())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getFullName())
                .studentEmail(e.getStudent().getEmail())
                .status(e.getStatus())
                .enrolledAt(e.getEnrolledAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveEnrollment(UUID courseId, UUID enrollmentId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        CourseEnrollment enrollment = courseEnrollmentRepository.findByIdAndCourseId(enrollmentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseEnrollment", enrollmentId));

        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        courseEnrollmentRepository.save(enrollment);
        log.info("Teacher {} approved enrollment {} for course {}", teacherId, enrollmentId, courseId);

        // Notify student via Kafka
        kafkaNotificationProducer.sendNotification(
                enrollment.getStudent().getId(),
                "ENROLLMENT_APPROVED",
                "Yêu cầu vào lớp đã được phê duyệt!",
                String.format("Giảng viên đã duyệt yêu cầu tham gia khóa học '%s' của bạn. Hãy vào học ngay nhé!", course.getName()),
                "/courses/" + course.getId()
        );
    }

    @Override
    @Transactional
    public void rejectEnrollment(UUID courseId, UUID enrollmentId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnershipOrTeaching(course, teacherId);

        CourseEnrollment enrollment = courseEnrollmentRepository.findByIdAndCourseId(enrollmentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseEnrollment", enrollmentId));

        enrollment.setStatus(EnrollmentStatus.REJECTED);
        courseEnrollmentRepository.save(enrollment);
        log.info("Teacher {} rejected enrollment {} for course {}", teacherId, enrollmentId, courseId);

        // Notify student via Kafka
        kafkaNotificationProducer.sendNotification(
                enrollment.getStudent().getId(),
                "ENROLLMENT_REJECTED",
                "Yêu cầu tham gia lớp học",
                String.format("Yêu cầu tham gia khóa học '%s' của bạn đã bị từ chối.", course.getName()),
                "/courses"
        );
    }

    private void verifyCourseOwnershipOrTeaching(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("You do not have permission to manage this course");
        }
    }

    private TeacherCourseResponse mapToCourseResponse(Course course) {
        return TeacherCourseResponse.builder()
                .id(course.getId())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .pricingType(course.getPricingType())
                .price(course.getPrice())
                .salePrice(course.getSalePrice())
                .currency(course.getCurrency())
                .publishedAt(course.getPublishedAt())
                .rejectReason(course.getRejectReason())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .enrollmentCount(course.getEnrollmentCount())
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .isDeleted(course.getIsDeleted())
                .deletedAt(course.getDeletedAt())
                .deletedBy(course.getDeletedBy())
                .build();
    }
}
