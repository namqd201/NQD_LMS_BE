package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorRequest;
import com.nqd.nqd_lms_be.dto.student.StudentStudyRecommendationDto;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentAiContextAssembler {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final QuestionRepository questionRepository;

    @Data
    @Builder
    public static class AssembledStudentContext {
        private String subjectName;
        private String courseTitle;
        private String lessonTitle;
        private String lessonSummary;
        private String lessonContent;
        private List<String> resourceNames;
        private String questionContent;
        private String studentAnswer;
        private Boolean isAnswerCorrect;
        private String questionExplanation;
        private Boolean isExamInProgress;
        private String formattedContext;
        @Builder.Default
        private List<StudentStudyRecommendationDto> recommendations = new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public AssembledStudentContext assemble(StudentAiTutorRequest request, UUID studentId) {
        StringBuilder sb = new StringBuilder();
        AssembledStudentContext.AssembledStudentContextBuilder builder = AssembledStudentContext.builder();
        List<StudentStudyRecommendationDto> recommendations = new ArrayList<>();

        // 1. Process Course & Enrollment verification
        if (request.getCourseId() != null) {
            Optional<Course> courseOpt = courseRepository.findById(request.getCourseId());
            if (courseOpt.isPresent()) {
                Course course = courseOpt.get();
                builder.courseTitle(course.getName());
                if (course.getSubject() != null) {
                    builder.subjectName(course.getSubject().getName());
                    sb.append("- Môn học: ").append(course.getSubject().getName()).append("\n");
                }
                sb.append("- Khóa học: ").append(course.getName()).append("\n");
                if (course.getDescription() != null && !course.getDescription().isBlank()) {
                    sb.append("- Mục tiêu khóa học: ").append(course.getDescription()).append("\n");
                }
            }
        }

        // 2. Process Lesson Context (Strict: only PUBLISHED lessons)
        if (request.getLessonId() != null) {
            Optional<Lesson> lessonOpt = lessonRepository.findById(request.getLessonId());
            if (lessonOpt.isPresent()) {
                Lesson lesson = lessonOpt.get();

                // Security Check: Must be PUBLISHED
                if (lesson.getStatus() != LessonStatus.PUBLISHED) {
                    log.warn("Student {} attempted to access unpublished lesson {}", studentId, lesson.getId());
                    throw new AccessDeniedException("Bạn không có quyền truy cập bài học chưa được xuất bản này.");
                }

                builder.lessonTitle(lesson.getTitle());
                builder.lessonSummary(lesson.getSummary());
                builder.lessonContent(lesson.getContent());
                sb.append("- Bài học hiện tại: ").append(lesson.getTitle()).append("\n");

                if (lesson.getChapter() != null) {
                    sb.append("- Chương: ").append(lesson.getChapter().getTitle()).append("\n");
                    if (lesson.getChapter().getCourse() != null) {
                        builder.courseTitle(lesson.getChapter().getCourse().getName());
                    }
                }

                if (lesson.getSummary() != null && !lesson.getSummary().isBlank()) {
                    sb.append("- Tóm tắt bài học: ").append(lesson.getSummary()).append("\n");
                }

                if (lesson.getContent() != null && !lesson.getContent().isBlank()) {
                    String cleanContent = lesson.getContent();
                    if (cleanContent.length() > 3000) {
                        cleanContent = cleanContent.substring(0, 3000) + "... (đã rút gọn nội dung)";
                    }
                    sb.append("- Nội dung bài giảng:\n").append(cleanContent).append("\n");
                }

                // Published resources
                List<LessonResource> resources = lessonResourceRepository.findByLessonIdOrderByDisplayOrderAsc(lesson.getId());
                if (!resources.isEmpty()) {
                    List<String> resNames = resources.stream().map(LessonResource::getTitle).toList();
                    builder.resourceNames(resNames);
                    sb.append("- Tài liệu học tập đính kèm: ").append(String.join(", ", resNames)).append("\n");
                }
            }
        }

        // 3. Process Exam Attempt & Question Context (Strict: student ownership verification)
        if (request.getExamAttemptId() != null) {
            Optional<ExamAttempt> attemptOpt = examAttemptRepository.findById(request.getExamAttemptId());
            if (attemptOpt.isPresent()) {
                ExamAttempt attempt = attemptOpt.get();

                // Security Check: Attempt must belong to authenticated student
                if (attempt.getStudent() == null || !attempt.getStudent().getId().equals(studentId)) {
                    log.warn("Student {} attempted to access exam attempt {} belonging to another user", studentId, attempt.getId());
                    throw new AccessDeniedException("Bạn không có quyền truy cập kết quả bài thi của học viên khác.");
                }

                boolean inProgress = (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS);
                builder.isExamInProgress(inProgress);

                if (inProgress) {
                    sb.append("\n[LƯU Ý BẢO MẬT BÀI THI]: Học sinh ĐANG TRONG THỜI GIAN LÀM BÀI KIỂM TRA.");
                    sb.append(" TUYỆT ĐỐI KHÔNG TIẾT LỘ ĐÁP ÁN TRỰC TIẾP (như A, B, C, D hay kết quả cuối cùng).");
                    sb.append(" Chỉ giải thích phương pháp, gợi mở tư duy theo phương pháp gợi ý sư phạm (Socratic hint).\n");
                }

                if (request.getQuestionId() != null) {
                    Optional<Question> qOpt = questionRepository.findById(request.getQuestionId());
                    if (qOpt.isPresent()) {
                        Question question = qOpt.get();
                        builder.questionContent(question.getContent());
                        sb.append("- Nội dung câu hỏi: ").append(question.getContent()).append("\n");

                        // Find student's answer for this attempt
                        Optional<ExamAnswer> answerOpt = examAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId());
                        if (answerOpt.isPresent()) {
                            ExamAnswer ans = answerOpt.get();
                            String studentChoice = ans.getSelectedOption() != null ?
                                    ans.getSelectedOption().getOptionKey() + ". " + ans.getSelectedOption().getOptionText() :
                                    ans.getAnswerText();

                            builder.studentAnswer(studentChoice);
                            builder.isAnswerCorrect(ans.getIsCorrect());
                            sb.append("- Câu trả lời của học sinh: ").append(studentChoice != null ? studentChoice : "Chưa chọn đáp án").append("\n");
                            if (!inProgress) {
                                sb.append("- Kết quả đánh giá: ").append(Boolean.TRUE.equals(ans.getIsCorrect()) ? "ĐÚNG" : "SAI").append("\n");
                                if (ans.getMarksAwarded() != null && ans.getMaxMarks() != null) {
                                    sb.append("- Điểm đạt được: ").append(ans.getMarksAwarded()).append("/").append(ans.getMaxMarks()).append("\n");
                                }
                            }
                        }

                        // Explanation is only given if NOT in progress or specifically requested
                        if (!inProgress && question.getExplanation() != null && !question.getExplanation().isBlank()) {
                            builder.questionExplanation(question.getExplanation());
                            sb.append("- Hướng dẫn giải chuẩn: ").append(question.getExplanation()).append("\n");
                        }
                    }
                }
            }
        }

        // 4. Process Study Recommendations if mode is RECOMMEND_STUDY
        if (request.getMode() == StudentAiTutorMode.RECOMMEND_STUDY) {
            recommendations = generateStudyRecommendations(studentId, request.getCourseId());
            builder.recommendations(recommendations);
            if (!recommendations.isEmpty()) {
                sb.append("\n- Các nội dung gợi ý nên ôn tập dựa trên kết quả học tập:\n");
                for (StudentStudyRecommendationDto rec : recommendations) {
                    sb.append(String.format("  + [%s] Khóa: '%s' - Bài: '%s' (%s) - Lý do: %s\n",
                            rec.getPriority(), rec.getCourseTitle(), rec.getLessonTitle(), rec.getTopic(), rec.getReason()));
                }
            }
        }

        builder.formattedContext(sb.toString());
        return builder.build();
    }

    private List<StudentStudyRecommendationDto> generateStudyRecommendations(UUID studentId, UUID specificCourseId) {
        List<StudentStudyRecommendationDto> recs = new ArrayList<>();
        List<CourseEnrollment> enrollments = (specificCourseId != null) ?
                enrollmentRepository.findByCourseIdAndStudentId(specificCourseId, studentId).map(List::of).orElse(List.of()) :
                enrollmentRepository.findByStudentId(studentId);

        for (CourseEnrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            if (course == null) continue;

            List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(studentId, course.getId());

            // Find published lessons
            List<Lesson> publishedLessons = lessonRepository.findByChapterCourseId(course.getId()).stream()
                    .filter(l -> l.getStatus() == LessonStatus.PUBLISHED)
                    .toList();

            for (Lesson lesson : publishedLessons) {
                boolean isDone = progresses.stream()
                        .anyMatch(p -> p.getLesson().getId().equals(lesson.getId()) && p.getStatus() == LessonProgressStatus.COMPLETED);

                if (!isDone) {
                    recs.add(StudentStudyRecommendationDto.builder()
                            .courseId(course.getId())
                            .courseTitle(course.getName())
                            .lessonId(lesson.getId())
                            .lessonTitle(lesson.getTitle())
                            .topic(lesson.getTitle())
                            .priority(recs.isEmpty() ? "HIGH" : "MEDIUM")
                            .reason("Bài học tiếp theo trong lộ trình khóa học chưa hoàn thành.")
                            .build());
                    if (recs.size() >= 3) break;
                }
            }
        }

        if (recs.isEmpty()) {
            recs.add(StudentStudyRecommendationDto.builder()
                    .topic("Ôn tập tổng hợp & Luyện đề")
                    .reason("Bạn đã hoàn thành tốt các bài học chính. Hãy thử sức với các đề kiểm tra thử để củng cố kiến thức!")
                    .priority("LOW")
                    .build());
        }

        return recs;
    }
}
