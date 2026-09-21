package com.nqd.nqd_lms_be.exercise;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.exercise.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherChapterRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherLessonRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.course.CourseWorkflowService;
import com.nqd.nqd_lms_be.service.student.StudentExerciseService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseStructureService;
import com.nqd.nqd_lms_be.service.teacher.TeacherExerciseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ExerciseIntegrationTest {

    @Autowired
    private TeacherExerciseService teacherExerciseService;

    @Autowired
    private StudentExerciseService studentExerciseService;

    @Autowired
    private TeacherCourseService teacherCourseService;

    @Autowired
    private TeacherCourseStructureService teacherCourseStructureService;

    @Autowired
    private CourseWorkflowService courseWorkflowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    private User teacher;
    private User student;
    private User admin;
    private Subject subject;
    private Course course;
    private Lesson lesson;
    private Question question1;
    private QuestionOption q1OptA;
    private QuestionOption q1OptB;
    private Question question2;
    private QuestionOption q2OptA;
    private QuestionOption q2OptB;

    @BeforeEach
    void setUp() {
        teacher = userRepository.save(User.builder()
                .email("teacher_exercise_" + UUID.randomUUID() + "@lms.com")
                .fullName("Teacher Exercise Test")
                .status(UserStatus.ACTIVE)
                .build());

        student = userRepository.save(User.builder()
                .email("student_exercise_" + UUID.randomUUID() + "@lms.com")
                .fullName("Student Exercise Test")
                .status(UserStatus.ACTIVE)
                .build());

        admin = userRepository.save(User.builder()
                .email("admin_exercise_" + UUID.randomUUID() + "@lms.com")
                .fullName("Admin Exercise Test")
                .status(UserStatus.ACTIVE)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Toán Học Exercise " + UUID.randomUUID().toString().substring(0, 5))
                .code("MATH_EX_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .build());

        // Create course
        authenticateAs(teacher, "ROLE_TEACHER");
        TeacherCourseResponse courseRes = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(subject.getId())
                        .name("Khóa Học Luyện Tập Toán")
                        .code("MATH-PRACTICE-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                        .gradeLevel("GRADE_10")
                        .pricingType(CoursePricingType.FREE)
                        .price(BigDecimal.ZERO)
                        .build(),
                teacher.getId()
        );

        var chRes = teacherCourseStructureService.createChapter(
                courseRes.getId(),
                TeacherChapterRequest.builder()
                        .title("Chương 1: Đại số")
                        .displayOrder(1)
                        .build(),
                teacher.getId()
        );

        var lsnRes = teacherCourseStructureService.createLesson(
                chRes.getId(),
                TeacherLessonRequest.builder()
                        .title("Bài 1: Phương trình bậc 2")
                        .displayOrder(1)
                        .estimatedMinutes(30)
                        .build(),
                teacher.getId()
        );

        courseWorkflowService.submitForReview(courseRes.getId(), teacher.getId());
        authenticateAs(admin, "ROLE_ADMIN");
        courseWorkflowService.approveCourse(courseRes.getId(), admin.getId());

        course = courseRepository.findById(courseRes.getId()).orElseThrow();
        lesson = lessonRepository.findById(lsnRes.getId()).orElseThrow();

        // Enroll student
        courseEnrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        // Create 2 questions with options
        question1 = questionRepository.save(Question.builder()
                .subject(subject)
                .course(course)
                .lesson(lesson)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("Giải phương trình: x^2 - 4 = 0?")
                .explanation("Phương trình x^2 - 4 = 0 <=> (x-2)(x+2)=0 <=> x = +-2.")
                .defaultMarks(BigDecimal.valueOf(5))
                .creator(teacher)
                .status(QuestionStatus.APPROVED)
                .build());

        q1OptA = questionOptionRepository.save(QuestionOption.builder()
                .question(question1)
                .optionKey("A")
                .optionText("x = 2 hoặc x = -2")
                .isCorrect(true)
                .displayOrder(1)
                .build());

        q1OptB = questionOptionRepository.save(QuestionOption.builder()
                .question(question1)
                .optionKey("B")
                .optionText("x = 4 hoặc x = -4")
                .isCorrect(false)
                .displayOrder(2)
                .build());

        question2 = questionRepository.save(Question.builder()
                .subject(subject)
                .course(course)
                .lesson(lesson)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("Nghiệm kép của phương trình x^2 - 2x + 1 = 0 là:")
                .explanation("Phương trình (x-1)^2 = 0 có nghiệm kép x = 1.")
                .defaultMarks(BigDecimal.valueOf(5))
                .creator(teacher)
                .status(QuestionStatus.APPROVED)
                .build());

        q2OptA = questionOptionRepository.save(QuestionOption.builder()
                .question(question2)
                .optionKey("A")
                .optionText("x = 1")
                .isCorrect(true)
                .displayOrder(1)
                .build());

        q2OptB = questionOptionRepository.save(QuestionOption.builder()
                .question(question2)
                .optionKey("B")
                .optionText("x = -1")
                .isCorrect(false)
                .displayOrder(2)
                .build());
    }

    private void authenticateAs(User user, String role) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of(role),
                Map.of("sub", user.getId().toString(), "email", user.getEmail()),
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Teacher CRUD and Publish Exercise with Questions")
    void testTeacherCrudAndPublishExercise() {
        authenticateAs(teacher, "ROLE_TEACHER");

        TeacherExerciseRequest createReq = TeacherExerciseRequest.builder()
                .lessonId(lesson.getId())
                .title("Bài tập luyện tập phương trình bậc 2")
                .description("Luyện tập các dạng bài tìm nghiệm")
                .instructions("Hãy chọn đáp án đúng nhất cho từng câu hỏi.")
                .type(ExerciseType.PRACTICE)
                .timeLimitMinutes(15)
                .passingScore(BigDecimal.valueOf(5))
                .status(ExerciseStatus.DRAFT)
                .maxAttempts(5)
                .showExplanationImmediately(true)
                .allowRetry(true)
                .questions(List.of(
                        TeacherExerciseQuestionItemRequest.builder().questionId(question1.getId()).marks(BigDecimal.valueOf(5)).displayOrder(1).build(),
                        TeacherExerciseQuestionItemRequest.builder().questionId(question2.getId()).marks(BigDecimal.valueOf(5)).displayOrder(2).build()
                ))
                .build();

        TeacherExerciseResponse created = teacherExerciseService.createExercise(createReq, teacher.getId());
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Bài tập luyện tập phương trình bậc 2");
        assertThat(created.getStatus()).isEqualTo(ExerciseStatus.DRAFT);
        assertThat(created.getQuestionCount()).isEqualTo(2);
        assertThat(created.getTotalMarks()).isEqualByComparingTo(BigDecimal.valueOf(10));

        // Publish exercise
        TeacherExerciseResponse published = teacherExerciseService.publishExercise(created.getId(), teacher.getId());
        assertThat(published.getStatus()).isEqualTo(ExerciseStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Student cannot view DRAFT exercise, but can view once PUBLISHED")
    void testStudentVisibilityDraftVsPublished() {
        authenticateAs(teacher, "ROLE_TEACHER");
        TeacherExerciseResponse exercise = teacherExerciseService.createExercise(
                TeacherExerciseRequest.builder()
                        .lessonId(lesson.getId())
                        .title("Draft Exercise")
                        .status(ExerciseStatus.DRAFT)
                        .questions(List.of(
                                TeacherExerciseQuestionItemRequest.builder().questionId(question1.getId()).marks(BigDecimal.valueOf(5)).build()
                        ))
                        .build(),
                teacher.getId()
        );

        authenticateAs(student, "ROLE_STUDENT");
        List<StudentExerciseSummaryResponse> available = studentExerciseService.getExercisesByLesson(lesson.getId(), student.getId());
        assertThat(available).noneMatch(e -> e.getId().equals(exercise.getId()));

        assertThatThrownBy(() -> studentExerciseService.getExerciseById(exercise.getId(), student.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // Teacher publishes it
        authenticateAs(teacher, "ROLE_TEACHER");
        teacherExerciseService.publishExercise(exercise.getId(), teacher.getId());

        // Now student sees it
        authenticateAs(student, "ROLE_STUDENT");
        available = studentExerciseService.getExercisesByLesson(lesson.getId(), student.getId());
        assertThat(available).anyMatch(e -> e.getId().equals(exercise.getId()));
    }

    @Test
    @DisplayName("Student starts attempt and gets instant auto-check per question")
    void testStudentPracticeFlowWithInstantAutoCheck() {
        // Teacher creates and publishes exercise
        authenticateAs(teacher, "ROLE_TEACHER");
        TeacherExerciseResponse exercise = teacherExerciseService.createExercise(
                TeacherExerciseRequest.builder()
                        .lessonId(lesson.getId())
                        .title("Bài tập tự kiểm tra")
                        .status(ExerciseStatus.DRAFT)
                        .questions(List.of(
                                TeacherExerciseQuestionItemRequest.builder().questionId(question1.getId()).marks(BigDecimal.valueOf(5)).displayOrder(1).build(),
                                TeacherExerciseQuestionItemRequest.builder().questionId(question2.getId()).marks(BigDecimal.valueOf(5)).displayOrder(2).build()
                        ))
                        .build(),
                teacher.getId()
        );
        teacherExerciseService.publishExercise(exercise.getId(), teacher.getId());

        // Student starts exercise
        authenticateAs(student, "ROLE_STUDENT");
        StudentExerciseTakingResponse takingRes = studentExerciseService.startExercise(exercise.getId(), student.getId());
        assertThat(takingRes).isNotNull();
        assertThat(takingRes.getAttemptNumber()).isEqualTo(1);
        assertThat(takingRes.getQuestions()).hasSize(2);

        UUID attemptId = takingRes.getAttemptId();

        // Answer Q1 correctly (Option A)
        StudentExerciseQuestionResultResponse q1Result = studentExerciseService.submitQuestion(
                attemptId,
                StudentExerciseSubmitQuestionRequest.builder()
                        .questionId(question1.getId())
                        .selectedOptionId(q1OptA.getId())
                        .build(),
                student.getId()
        );
        assertThat(q1Result.getIsCorrect()).isTrue();
        assertThat(q1Result.getMarksAwarded()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(q1Result.getCorrectOptionKey()).isEqualTo("A");
        assertThat(q1Result.getExplanation()).contains("x^2 - 4 = 0");

        // Answer Q2 incorrectly (Option B)
        StudentExerciseQuestionResultResponse q2Result = studentExerciseService.submitQuestion(
                attemptId,
                StudentExerciseSubmitQuestionRequest.builder()
                        .questionId(question2.getId())
                        .selectedOptionId(q2OptB.getId())
                        .build(),
                student.getId()
        );
        assertThat(q2Result.getIsCorrect()).isFalse();
        assertThat(q2Result.getMarksAwarded()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(q2Result.getCorrectOptionKey()).isEqualTo("A");

        // Submit entire attempt
        StudentExerciseAttemptResultResponse finalResult = studentExerciseService.submitAttempt(attemptId, student.getId());
        assertThat(finalResult.getStatus()).isEqualTo(ExerciseAttemptStatus.COMPLETED);
        assertThat(finalResult.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(finalResult.getMaxScore()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(finalResult.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(finalResult.getCorrectCount()).isEqualTo(1);
        assertThat(finalResult.getTotalQuestions()).isEqualTo(2);

        // Check past attempts
        List<StudentExerciseAttemptResultResponse> myAttempts = studentExerciseService.getMyAttempts(exercise.getId(), student.getId());
        assertThat(myAttempts).hasSize(1);
        assertThat(myAttempts.get(0).getAttemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Student calls AI explanation for a question")
    void testStudentAiExplanation() {
        authenticateAs(teacher, "ROLE_TEACHER");
        TeacherExerciseResponse exercise = teacherExerciseService.createExercise(
                TeacherExerciseRequest.builder()
                        .lessonId(lesson.getId())
                        .title("Exercise AI Test")
                        .status(ExerciseStatus.PUBLISHED)
                        .questions(List.of(
                                TeacherExerciseQuestionItemRequest.builder().questionId(question1.getId()).marks(BigDecimal.valueOf(5)).build()
                        ))
                        .build(),
                teacher.getId()
        );

        authenticateAs(student, "ROLE_STUDENT");
        StudentExerciseTakingResponse takingRes = studentExerciseService.startExercise(exercise.getId(), student.getId());
        studentExerciseService.submitQuestion(
                takingRes.getAttemptId(),
                StudentExerciseSubmitQuestionRequest.builder()
                        .questionId(question1.getId())
                        .selectedOptionId(q1OptB.getId())
                        .build(),
                student.getId()
        );

        StudentExerciseAiExplainResponse aiRes = studentExerciseService.explainQuestionWithAi(
                takingRes.getAttemptId(),
                question1.getId(),
                "Giải thích cho tôi tại sao đáp án B sai",
                student.getId()
        );

        assertThat(aiRes).isNotNull();
        assertThat(aiRes.getQuestionId()).isEqualTo(question1.getId());
        assertThat(aiRes.getExplanation()).isNotBlank();
    }
}
