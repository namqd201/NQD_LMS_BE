package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.StudentChapterResponse;
import com.nqd.nqd_lms_be.dto.student.StudentCourseDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonSummaryResponse;
import com.nqd.nqd_lms_be.entity.Chapter;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Lesson;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.ChapterRepository;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.repository.LessonRepository;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentCourseStructureServiceImpl implements StudentCourseStructureService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final com.nqd.nqd_lms_be.repository.EntitlementRepository entitlementRepository;
    private final com.nqd.nqd_lms_be.repository.LessonProgressRepository lessonProgressRepository;
    private final com.nqd.nqd_lms_be.repository.ExerciseRepository exerciseRepository;
    private final com.nqd.nqd_lms_be.repository.ExerciseAttemptRepository exerciseAttemptRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public StudentCourseDetailResponse getPublishedCourseDetail(UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // PUBLISHING RULE: Unpublished content must not be visible to students
        if (!course.isPublished()) {
            throw new ForbiddenOperationException("Course is not published or available for students");
        }

        boolean isOwner = course.getCreator() != null && course.getCreator().getId().equals(studentId);
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isEnrolled = isOwner || isAdmin || courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId) ||
                entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                        studentId, com.nqd.nqd_lms_be.entity.enums.EntitlementType.COURSE_ACCESS, courseId, com.nqd.nqd_lms_be.entity.enums.EntitlementStatus.ACTIVE
                ).isPresent();

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAsc(courseId);
        List<StudentChapterResponse> chapterDtos = new ArrayList<>();

        // Preload student progress & attempts for course
        List<com.nqd.nqd_lms_be.entity.LessonProgress> progresses = (studentId != null)
                ? lessonProgressRepository.findByStudentIdAndCourseId(studentId, courseId)
                : Collections.emptyList();
        Map<UUID, com.nqd.nqd_lms_be.entity.LessonProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p, (p1, p2) -> p1));

        List<com.nqd.nqd_lms_be.entity.ExerciseAttempt> studentAttempts = (studentId != null)
                ? exerciseAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId)
                : Collections.emptyList();

        // Flatten all published lessons in course order to evaluate sequential locking
        List<Lesson> allCourseLessons = new ArrayList<>();
        Map<UUID, List<Lesson>> chapterLessonsMap = new LinkedHashMap<>();
        for (Chapter ch : chapters) {
            List<Lesson> pubLessons = lessonRepository.findByChapterIdAndStatusOrderByDisplayOrderAsc(ch.getId(), LessonStatus.PUBLISHED);
            chapterLessonsMap.put(ch.getId(), pubLessons);
            allCourseLessons.addAll(pubLessons);
        }

        // Compute completion & pass status for each lesson
        Map<UUID, Boolean> lessonCompletedMap = new HashMap<>();
        Map<UUID, Boolean> lessonExercisesPassedMap = new HashMap<>();
        for (Lesson l : allCourseLessons) {
            com.nqd.nqd_lms_be.entity.LessonProgress lp = progressMap.get(l.getId());
            boolean completed = lp != null && lp.getStatus() == com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED;
            lessonCompletedMap.put(l.getId(), completed);

            List<com.nqd.nqd_lms_be.entity.Exercise> exercises = exerciseRepository.findByLessonIdAndStatusAndIsDeletedFalse(l.getId(), com.nqd.nqd_lms_be.entity.enums.ExerciseStatus.PUBLISHED);
            boolean allPassed = true;
            if (!exercises.isEmpty()) {
                for (com.nqd.nqd_lms_be.entity.Exercise ex : exercises) {
                    boolean exPassed = studentAttempts.stream()
                            .anyMatch(att -> att.getExercise().getId().equals(ex.getId()) &&
                                    att.getStatus() == com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus.COMPLETED &&
                                    Boolean.TRUE.equals(att.getPassed()));
                    if (!exPassed) {
                        allPassed = false;
                        break;
                    }
                }
            }
            lessonExercisesPassedMap.put(l.getId(), allPassed);
        }

        // Sequential locking evaluation
        Map<UUID, Boolean> lessonLockedMap = new HashMap<>();
        Map<UUID, String> lessonLockReasonMap = new HashMap<>();

        for (int i = 0; i < allCourseLessons.size(); i++) {
            Lesson l = allCourseLessons.get(i);
            com.nqd.nqd_lms_be.entity.LessonProgress lp = progressMap.get(l.getId());

            if (isAdmin || isOwner) {
                lessonLockedMap.put(l.getId(), false);
                continue;
            }

            boolean isUltra = studentId != null && membershipEntitlementService.hasFeature(studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.ULTRA_UNLIMITED_COURSES);
            boolean hasChapterAccess = studentId != null && entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                    studentId, com.nqd.nqd_lms_be.entity.enums.EntitlementType.CHAPTER_ACCESS, l.getChapter().getId(), com.nqd.nqd_lms_be.entity.enums.EntitlementStatus.ACTIVE
            ).isPresent();
            boolean hasLessonAccess = studentId != null && entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                    studentId, com.nqd.nqd_lms_be.entity.enums.EntitlementType.LESSON_ACCESS, l.getId(), com.nqd.nqd_lms_be.entity.enums.EntitlementStatus.ACTIVE
            ).isPresent();

            boolean hasPaidAccess = isEnrolled || isUltra || hasChapterAccess || hasLessonAccess;

            if (course.isPaid() && !hasPaidAccess && !Boolean.TRUE.equals(l.getIsPreview())) {
                lessonLockedMap.put(l.getId(), true);
                lessonLockReasonMap.put(l.getId(), "Vui lòng mua khóa học, chương hoặc bài học này để mở khóa.");
                continue;
            }

            if (Boolean.TRUE.equals(l.getIsPreview()) || (lp != null && Boolean.TRUE.equals(lp.getIsUnlockedByAdmin()))) {
                lessonLockedMap.put(l.getId(), false);
                continue;
            }

            // CRITICAL: Any lesson that the student has already completed must ALWAYS remain unlocked so they can review it!
            boolean currentCompleted = lessonCompletedMap.getOrDefault(l.getId(), false);
            if (currentCompleted) {
                lessonLockedMap.put(l.getId(), false);
                continue;
            }

            if (i == 0) {
                lessonLockedMap.put(l.getId(), false);
            } else {
                Lesson prevLesson = allCourseLessons.get(i - 1);
                boolean prevCompleted = lessonCompletedMap.getOrDefault(prevLesson.getId(), false);
                boolean prevExercisesPassed = lessonExercisesPassedMap.getOrDefault(prevLesson.getId(), true);

                if (!prevCompleted || !prevExercisesPassed) {
                    lessonLockedMap.put(l.getId(), true);
                    if (!prevCompleted) {
                        lessonLockReasonMap.put(l.getId(), "Bạn cần hoàn thành bài học trước \"" + prevLesson.getTitle() + "\" để mở khóa bài học này.");
                    } else {
                        lessonLockReasonMap.put(l.getId(), "Bạn cần làm đúng 100% tất cả bài tập của bài \"" + prevLesson.getTitle() + "\" để mở khóa bài học này.");
                    }
                } else {
                    lessonLockedMap.put(l.getId(), false);
                }
            }
        }

        for (Chapter ch : chapters) {
            List<Lesson> publishedLessons = chapterLessonsMap.getOrDefault(ch.getId(), Collections.emptyList());

            List<StudentLessonSummaryResponse> lessonDtos = publishedLessons.stream()
                    .map(l -> StudentLessonSummaryResponse.builder()
                            .id(l.getId())
                            .title(l.getTitle())
                            .slug(l.getSlug())
                            .summary(l.getSummary())
                            .displayOrder(l.getDisplayOrder())
                            .estimatedMinutes(l.getEstimatedMinutes())
                            .status(l.getStatus())
                            .isPreview(Boolean.TRUE.equals(l.getIsPreview()))
                            .isLocked(lessonLockedMap.getOrDefault(l.getId(), false))
                            .lockReason(lessonLockReasonMap.get(l.getId()))
                            .isCompleted(lessonCompletedMap.getOrDefault(l.getId(), false))
                            .isExercisesPassed(lessonExercisesPassedMap.getOrDefault(l.getId(), true))
                            .build())
                    .collect(Collectors.toList());

            chapterDtos.add(StudentChapterResponse.builder()
                    .id(ch.getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .displayOrder(ch.getDisplayOrder())
                    .lessons(lessonDtos)
                    .build());
        }

        return StudentCourseDetailResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .isOwner(isOwner)
                .isEnrolled(isEnrolled)
                .chapters(chapterDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentLessonDetailResponse getPublishedLesson(UUID lessonId, UUID studentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        // PUBLISHING RULE: Lesson must be PUBLISHED and its parent course must be published
        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new ForbiddenOperationException("Lesson is not published or available");
        }
        Course course = lesson.getChapter() != null ? lesson.getChapter().getCourse() : null;
        if (course == null || !course.isPublished()) {
            throw new ForbiddenOperationException("The course containing this lesson is not published");
        }

        boolean isOwner = course.getCreator() != null && course.getCreator().getId().equals(studentId);
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isEnrolled = isOwner || isAdmin || courseEnrollmentRepository.existsByCourseIdAndStudentId(course.getId(), studentId) ||
                entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                        studentId, com.nqd.nqd_lms_be.entity.enums.EntitlementType.COURSE_ACCESS, course.getId(), com.nqd.nqd_lms_be.entity.enums.EntitlementStatus.ACTIVE
                ).isPresent();

        // Gating: If course is paid and student is not enrolled/owner/admin, only allow if isPreview is true
        if (course.isPaid() && !isEnrolled && !isAdmin) {
            if (!Boolean.TRUE.equals(lesson.getIsPreview())) {
                throw new ForbiddenOperationException("Vui lòng mua khóa học để mở khóa bài học này.");
            }
        }

        boolean isLocked = false;
        String lockReason = null;

        if (!isAdmin && !isOwner && !Boolean.TRUE.equals(lesson.getIsPreview())) {
            com.nqd.nqd_lms_be.entity.LessonProgress currentLp = (studentId != null)
                    ? lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId).orElse(null)
                    : null;

            boolean currentCompleted = currentLp != null && currentLp.getStatus() == com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED;
            boolean isUnlockedByAdmin = currentLp != null && Boolean.TRUE.equals(currentLp.getIsUnlockedByAdmin());

            if (currentCompleted || isUnlockedByAdmin) {
                isLocked = false;
            } else {
                List<Chapter> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAsc(course.getId());
                List<Lesson> allCourseLessons = new ArrayList<>();
                for (Chapter ch : chapters) {
                    allCourseLessons.addAll(lessonRepository.findByChapterIdAndStatusOrderByDisplayOrderAsc(ch.getId(), LessonStatus.PUBLISHED));
                }

                int index = -1;
                for (int i = 0; i < allCourseLessons.size(); i++) {
                    if (allCourseLessons.get(i).getId().equals(lessonId)) {
                        index = i;
                        break;
                    }
                }

                if (index == 0) {
                    isLocked = false;
                } else if (index > 0) {
                    Lesson prevLesson = allCourseLessons.get(index - 1);
                    com.nqd.nqd_lms_be.entity.LessonProgress prevLp = (studentId != null)
                            ? lessonProgressRepository.findByStudentIdAndLessonId(studentId, prevLesson.getId()).orElse(null)
                            : null;

                    boolean prevCompleted = prevLp != null && prevLp.getStatus() == com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED;
                    if (!prevCompleted) {
                        isLocked = true;
                        lockReason = "Bạn cần hoàn thành bài học trước \"" + prevLesson.getTitle() + "\" để mở khóa bài học này.";
                    } else {
                        List<com.nqd.nqd_lms_be.entity.Exercise> exercises = exerciseRepository.findByLessonIdAndStatusAndIsDeletedFalse(prevLesson.getId(), com.nqd.nqd_lms_be.entity.enums.ExerciseStatus.PUBLISHED);
                        if (!exercises.isEmpty() && studentId != null) {
                            List<com.nqd.nqd_lms_be.entity.ExerciseAttempt> studentAttempts = exerciseAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
                            for (com.nqd.nqd_lms_be.entity.Exercise ex : exercises) {
                                boolean exPassed = studentAttempts.stream()
                                        .anyMatch(att -> att.getExercise().getId().equals(ex.getId()) &&
                                                att.getStatus() == com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus.COMPLETED &&
                                                Boolean.TRUE.equals(att.getPassed()));
                                if (!exPassed) {
                                    isLocked = true;
                                    lockReason = "Bạn cần làm đúng 100% tất cả bài tập của bài \"" + prevLesson.getTitle() + "\" để mở khóa bài học này.";
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLocked) {
            throw new ForbiddenOperationException(lockReason != null ? lockReason : "Bài học này hiện đang bị khóa.");
        }

        return StudentLessonDetailResponse.builder()
                .id(lesson.getId())
                .chapterId(lesson.getChapter().getId())
                .chapterTitle(lesson.getChapter().getTitle())
                .courseId(lesson.getChapter().getCourse().getId())
                .courseName(lesson.getChapter().getCourse().getName())
                .title(lesson.getTitle())
                .slug(lesson.getSlug())
                .summary(lesson.getSummary())
                .content(lesson.getContent())
                .displayOrder(lesson.getDisplayOrder())
                .estimatedMinutes(lesson.getEstimatedMinutes())
                .isPreview(Boolean.TRUE.equals(lesson.getIsPreview()))
                .videoUrl(lesson.getVideoUrl())
                .isLocked(isLocked)
                .lockReason(lockReason)
                .build();
    }
}
