package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.TeacherStudentDetailProgressResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherStudentLessonProgressResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherStudentProgressServiceImpl implements TeacherStudentProgressService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherStudentLessonProgressResponse> getCourseStudentProgress(UUID teacherId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);

        List<LessonProgress> progresses = lessonProgressRepository.findByCourseId(courseId);
        return progresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherStudentLessonProgressResponse> getLessonStudentProgress(UUID teacherId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        List<LessonProgress> progresses = lessonProgressRepository.findByLessonId(lessonId);
        return progresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherStudentDetailProgressResponse getStudentProgressDetail(UUID teacherId, UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdWithCourse(studentId);
        List<TeacherStudentDetailProgressResponse.StudentCourseDetailDto> courseDetails = new ArrayList<>();

        long totalCompletedLessons = 0;
        int completedCoursesCount = 0;
        double sumOverallExamScores = 0.0;
        int countOverallExams = 0;
        LocalDateTime lastActive = null;

        for (CourseEnrollment ce : enrollments) {
            Course course = ce.getCourse();

            boolean canViewCourse = SecurityUtils.isAdmin()
                    || (course.getCreator() != null && course.getCreator().getId().equals(teacherId))
                    || courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

            if (!canViewCourse) {
                continue;
            }

            long totalLessonsInCourse = lessonRepository.countByCourseId(course.getId());
            List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(studentId, course.getId());
            List<ExamAttempt> attempts = examAttemptRepository.findByStudentIdAndCourseId(studentId, course.getId());

            long completedInCourse = progresses.stream()
                    .filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED)
                    .count();

            totalCompletedLessons += completedInCourse;
            if (ce.getStatus() == EnrollmentStatus.COMPLETED) {
                completedCoursesCount++;
            }

            BigDecimal progressPercent = totalLessonsInCourse > 0
                    ? BigDecimal.valueOf(completedInCourse).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalLessonsInCourse), 1, RoundingMode.HALF_UP)
                    : (ce.getStatus() == EnrollmentStatus.COMPLETED ? new BigDecimal("100.0") : BigDecimal.ZERO);

            Double avgCourseExamScore = null;
            if (!attempts.isEmpty()) {
                double sum = attempts.stream()
                        .filter(a -> a.getPercentage() != null)
                        .mapToDouble(a -> a.getPercentage().doubleValue())
                        .sum();
                long count = attempts.stream().filter(a -> a.getPercentage() != null).count();
                if (count > 0) {
                    avgCourseExamScore = Math.round((sum / count) * 10.0) / 10.0;
                    sumOverallExamScores += sum;
                    countOverallExams += count;
                }
            }

            List<TeacherStudentDetailProgressResponse.StudentLessonItemDto> lessonDtos = progresses.stream()
                    .map(p -> TeacherStudentDetailProgressResponse.StudentLessonItemDto.builder()
                            .lessonId(p.getLesson().getId())
                            .lessonTitle(p.getLesson().getTitle())
                            .chapterTitle(p.getLesson().getChapter() != null ? p.getLesson().getChapter().getTitle() : null)
                            .status(p.getStatus().name())
                            .progressPercent(p.getProgressPercent())
                            .lastAccessedAt(p.getLastAccessedAt())
                            .completedAt(p.getCompletedAt())
                            .isUnlockedByAdmin(Boolean.TRUE.equals(p.getIsUnlockedByAdmin()))
                            .build())
                    .collect(Collectors.toList());

            List<TeacherStudentDetailProgressResponse.StudentExamAttemptDto> attemptDtos = attempts.stream()
                    .map(a -> TeacherStudentDetailProgressResponse.StudentExamAttemptDto.builder()
                            .attemptId(a.getId())
                            .examId(a.getExam().getId())
                            .examTitle(a.getExam().getTitle())
                            .attemptNumber(a.getAttemptNumber())
                            .totalScore(a.getTotalScore())
                            .percentage(a.getPercentage())
                            .passed(a.getPassed())
                            .submittedAt(a.getSubmittedAt())
                            .build())
                    .collect(Collectors.toList());

            for (LessonProgress p : progresses) {
                if (p.getLastAccessedAt() != null && (lastActive == null || p.getLastAccessedAt().isAfter(lastActive))) {
                    lastActive = p.getLastAccessedAt();
                }
            }

            courseDetails.add(TeacherStudentDetailProgressResponse.StudentCourseDetailDto.builder()
                    .courseId(course.getId())
                    .courseName(course.getName())
                    .courseThumbnail(course.getThumbnailUrl())
                    .enrollmentStatus(ce.getStatus().name())
                    .enrolledAt(ce.getEnrolledAt())
                    .completedAt(ce.getCompletedAt())
                    .progressPercent(progressPercent)
                    .completedLessonsCount(completedInCourse)
                    .totalLessonsCount(totalLessonsInCourse)
                    .averageExamScore(avgCourseExamScore)
                    .lessonProgresses(lessonDtos)
                    .examAttempts(attemptDtos)
                    .build());
        }

        Double overallAvgExamScore = countOverallExams > 0 ? Math.round((sumOverallExamScores / countOverallExams) * 10.0) / 10.0 : null;

        return TeacherStudentDetailProgressResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .avatarUrl(student.getAvatarUrl())
                .phone(student.getPhoneNumber())
                .enrolledCoursesCount(courseDetails.size())
                .completedCoursesCount(completedCoursesCount)
                .totalCompletedLessons(totalCompletedLessons)
                .overallAverageExamScore(overallAvgExamScore)
                .lastActiveAt(lastActive)
                .courses(courseDetails)
                .build();
    }

    @Override
    @Transactional
    public void unlockLesson(UUID teacherId, UUID courseId, UUID studentId, UUID lessonId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        verifyCourseOwnership(course, teacherId);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        LessonProgress progress = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .startedAt(LocalDateTime.now())
                        .status(LessonProgressStatus.IN_PROGRESS)
                        .progressPercent(BigDecimal.ZERO)
                        .lastAccessedAt(LocalDateTime.now())
                        .build());

        progress.setIsUnlockedByAdmin(true);
        lessonProgressRepository.save(progress);
        log.info("Teacher/Admin {} manually unlocked lesson {} for student {}", teacherId, lessonId, studentId);
    }

    @Override
    @Transactional
    public void unlockAllLessons(UUID teacherId, UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        verifyCourseOwnership(course, teacherId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        List<Lesson> lessons = lessonRepository.findByChapterCourseId(courseId);
        for (Lesson lesson : lessons) {
            LessonProgress progress = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lesson.getId())
                    .orElseGet(() -> LessonProgress.builder()
                            .student(student)
                            .lesson(lesson)
                            .startedAt(LocalDateTime.now())
                            .status(LessonProgressStatus.IN_PROGRESS)
                            .progressPercent(BigDecimal.ZERO)
                            .lastAccessedAt(LocalDateTime.now())
                            .build());

            progress.setIsUnlockedByAdmin(true);
            lessonProgressRepository.save(progress);
        }
        log.info("Teacher/Admin {} manually unlocked all {} lessons in course {} for student {}", teacherId, lessons.size(), courseId, studentId);
    }

    private void verifyCourseOwnership(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("You do not have permission to view student progress in this course");
        }
    }

    private TeacherStudentLessonProgressResponse mapToResponse(LessonProgress lp) {
        return TeacherStudentLessonProgressResponse.builder()
                .progressId(lp.getId())
                .studentId(lp.getStudent().getId())
                .studentName(lp.getStudent().getFullName())
                .studentEmail(lp.getStudent().getEmail())
                .lessonId(lp.getLesson().getId())
                .lessonTitle(lp.getLesson().getTitle())
                .courseId(lp.getLesson().getChapter().getCourse().getId())
                .status(lp.getStatus())
                .progressPercent(lp.getProgressPercent())
                .startedAt(lp.getStartedAt())
                .completedAt(lp.getCompletedAt())
                .lastAccessedAt(lp.getLastAccessedAt())
                .isUnlockedByAdmin(Boolean.TRUE.equals(lp.getIsUnlockedByAdmin()))
                .build();
    }
}
