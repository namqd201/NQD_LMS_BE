package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.StudentCourseProgressResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonProgressResponse;
import com.nqd.nqd_lms_be.dto.student.StudentOverallAnalyticsResponse;
import com.nqd.nqd_lms_be.dto.student.UpdateLessonProgressRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.certificate.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentProgressServiceImpl implements StudentProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CertificateService certificateService;

    @Override
    @Transactional(readOnly = true)
    public StudentLessonProgressResponse getLessonProgress(UUID studentId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyPublishedAccess(lesson);

        Optional<LessonProgress> progressOpt = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId);
        return progressOpt.map(this::mapToResponse).orElseGet(() -> StudentLessonProgressResponse.builder()
                .lessonId(lessonId)
                .status(LessonProgressStatus.IN_PROGRESS)
                .progressPercent(BigDecimal.ZERO)
                .startedAt(null)
                .completedAt(null)
                .lastAccessedAt(null)
                .build());
    }

    @Override
    @Transactional
    public StudentLessonProgressResponse updateLessonProgress(UUID studentId, UUID lessonId, UpdateLessonProgressRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyPublishedAccess(lesson);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        // Ensure CourseEnrollment exists for active non-private course
        UUID courseId = lesson.getChapter() != null && lesson.getChapter().getCourse() != null
                ? lesson.getChapter().getCourse().getId()
                : null;
        if (courseId != null) {
            Course course = lesson.getChapter().getCourse();
            if (!courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)
                    && course.getStatus() == CourseStatus.ACTIVE
                    && !Boolean.TRUE.equals(course.getIsPrivate())) {
                CourseEnrollment autoEnrollment = CourseEnrollment.builder()
                        .course(course)
                        .student(student)
                        .status(EnrollmentStatus.ENROLLED)
                        .enrolledAt(LocalDateTime.now())
                        .build();
                courseEnrollmentRepository.save(autoEnrollment);
                log.info("Auto-enrolled student {} in course {}", studentId, courseId);
            }
        }

        LessonProgress progress = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .startedAt(LocalDateTime.now())
                        .status(LessonProgressStatus.IN_PROGRESS)
                        .progressPercent(BigDecimal.ZERO)
                        .build());

        if (progress.getStartedAt() == null) {
            progress.setStartedAt(LocalDateTime.now());
        }
        progress.setLastAccessedAt(LocalDateTime.now());

        boolean isCompletedRequested = Boolean.TRUE.equals(request.getCompleted());
        BigDecimal requestedPercent = request.getProgressPercent();

        if (isCompletedRequested || (requestedPercent != null && requestedPercent.compareTo(new BigDecimal("100.00")) >= 0)) {
            progress.setStatus(LessonProgressStatus.COMPLETED);
            progress.setProgressPercent(new BigDecimal("100.00"));
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        } else if (requestedPercent != null) {
            progress.setProgressPercent(requestedPercent);
            if (requestedPercent.compareTo(new BigDecimal("100.00")) < 0) {
                progress.setStatus(LessonProgressStatus.IN_PROGRESS);
                progress.setCompletedAt(null);
            }
        }

        if (request.getVideoWatched() != null) {
            progress.setVideoWatched(request.getVideoWatched());
        }

        progress = lessonProgressRepository.save(progress);
        log.info("Student {} updated progress on lesson {} to {}% (videoWatched: {})",
                studentId, lessonId, progress.getProgressPercent(), progress.getVideoWatched());

        // Check if course is 100% completed across all criteria to automatically issue certificate
        if (courseId != null && (progress.getStatus() == LessonProgressStatus.COMPLETED || Boolean.TRUE.equals(progress.getVideoWatched()))) {
            certificateService.checkAndAutoIssueCertificate(studentId, courseId);
        }

        return mapToResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentCourseProgressResponse getCourseProgress(UUID studentId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ForbiddenOperationException("Cannot view progress on unpublished course");
        }

        List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(studentId, courseId);
        long completedCount = progresses.stream()
                .filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED)
                .count();

        List<StudentLessonProgressResponse> lessonResponses = progresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        long totalLessons = lessonRepository.countByCourseId(courseId);
        BigDecimal overallPercent = totalLessons > 0
                ? BigDecimal.valueOf(completedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StudentCourseProgressResponse.builder()
                .courseId(courseId)
                .overallProgressPercent(overallPercent)
                .totalLessons(totalLessons)
                .completedLessons(completedCount)
                .lessonProgresses(lessonResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentOverallAnalyticsResponse getStudentOverallAnalytics(UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        // 1. Study time & completed lessons
        Long totalEstMinutes = lessonProgressRepository.sumCompletedEstimatedMinutesByStudentId(studentId);
        double totalStudyHours = Math.round(((totalEstMinutes != null ? totalEstMinutes : 0L) / 60.0) * 10.0) / 10.0;
        long totalCompletedLessons = lessonProgressRepository.countByStudentIdAndStatus(studentId, LessonProgressStatus.COMPLETED);

        // 2. Course counts
        long totalEnrolledCourses = courseEnrollmentRepository.countByStudentId(studentId);
        long totalCompletedCourses = courseEnrollmentRepository.countByStudentIdAndStatus(studentId, EnrollmentStatus.COMPLETED);

        // 3. Exam performance
        Double avgExamScore = examAttemptRepository.findAveragePercentageByStudentId(studentId);
        if (avgExamScore != null) {
            avgExamScore = Math.round(avgExamScore * 10.0) / 10.0;
        }
        long totalExamsTaken = examAttemptRepository.countByStudentIdAndStatus(studentId, ExamAttemptStatus.SUBMITTED);
        long totalExamsPassed = examAttemptRepository.countByStudentIdAndPassed(studentId, true);

        // 4. Streak calculation
        List<java.sql.Date> dateRows = lessonProgressRepository.findDistinctActivityDatesByStudentId(studentId);
        List<LocalDate> activityDates = dateRows.stream()
                .map(java.sql.Date::toLocalDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int currentStreak = 0;
        int longestStreak = 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (!activityDates.isEmpty()) {
            LocalDate mostRecent = activityDates.get(0);
            if (mostRecent.equals(today) || mostRecent.equals(yesterday)) {
                LocalDate expected = mostRecent;
                for (LocalDate d : activityDates) {
                    if (d.equals(expected)) {
                        currentStreak++;
                        expected = expected.minusDays(1);
                    } else if (d.isBefore(expected)) {
                        break;
                    }
                }
            }

            // Calculate longest streak across all history
            List<LocalDate> sortedAsc = dateRows.stream()
                    .map(java.sql.Date::toLocalDate)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            int tempStreak = 0;
            LocalDate prev = null;
            for (LocalDate d : sortedAsc) {
                if (prev == null || d.equals(prev.plusDays(1))) {
                    tempStreak++;
                } else if (!d.equals(prev)) {
                    tempStreak = 1;
                }
                prev = d;
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak;
                }
            }
        }

        // 5. Activity Heatmap (Last 365 days)
        LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
        List<Object[]> dailyCounts = lessonProgressRepository.findDailyActivityCountsByStudentId(studentId, oneYearAgo);
        List<StudentOverallAnalyticsResponse.DailyHeatmapItemDto> heatmap = new ArrayList<>();
        for (Object[] row : dailyCounts) {
            if (row[0] != null) {
                String dStr = row[0].toString();
                int count = ((Number) row[1]).intValue();
                heatmap.add(StudentOverallAnalyticsResponse.DailyHeatmapItemDto.builder()
                        .date(dStr)
                        .count(count)
                        .build());
            }
        }

        // 6. Recent courses progress
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdWithCourse(studentId);
        List<StudentOverallAnalyticsResponse.StudentCourseProgressSummaryDto> recentCourses = new ArrayList<>();
        for (CourseEnrollment ce : enrollments) {
            Course course = ce.getCourse();
            long totalCourseLessons = lessonRepository.countByCourseId(course.getId());
            List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(studentId, course.getId());
            long completed = progresses.stream().filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED).count();

            BigDecimal progressPercent = totalCourseLessons > 0
                    ? BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalCourseLessons), 1, RoundingMode.HALF_UP)
                    : (ce.getStatus() == EnrollmentStatus.COMPLETED ? new BigDecimal("100.0") : BigDecimal.ZERO);

            recentCourses.add(StudentOverallAnalyticsResponse.StudentCourseProgressSummaryDto.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getName())
                    .courseThumbnail(course.getThumbnailUrl())
                    .progressPercent(progressPercent)
                    .completedLessons(completed)
                    .totalLessons(totalCourseLessons)
                    .enrollmentStatus(ce.getStatus().name())
                    .build());
        }

        return StudentOverallAnalyticsResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .totalStudyHours(totalStudyHours)
                .totalCompletedLessons(totalCompletedLessons)
                .totalEnrolledCourses(totalEnrolledCourses)
                .totalCompletedCourses(totalCompletedCourses)
                .averageExamScore(avgExamScore)
                .totalExamsTaken(totalExamsTaken)
                .totalExamsPassed(totalExamsPassed)
                .currentStreakDays(currentStreak)
                .longestStreakDays(longestStreak)
                .activityHeatmap(heatmap)
                .recentCourses(recentCourses)
                .build();
    }

    private void verifyPublishedAccess(Lesson lesson) {
        Course course = lesson.getChapter().getCourse();
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ForbiddenOperationException("Cannot access progress on unpublished course");
        }
        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new ForbiddenOperationException("Cannot access progress on unpublished lesson");
        }
    }

    private StudentLessonProgressResponse mapToResponse(LessonProgress p) {
        return StudentLessonProgressResponse.builder()
                .progressId(p.getId())
                .lessonId(p.getLesson().getId())
                .status(p.getStatus())
                .progressPercent(p.getProgressPercent())
                .startedAt(p.getStartedAt())
                .completedAt(p.getCompletedAt())
                .lastAccessedAt(p.getLastAccessedAt())
                .videoWatched(Boolean.TRUE.equals(p.getVideoWatched()))
                .build();
    }
}
