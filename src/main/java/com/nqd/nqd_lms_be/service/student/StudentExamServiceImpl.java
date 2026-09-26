package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionOptionDto;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptEventType;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.GradingMethod;
import com.nqd.nqd_lms_be.entity.enums.GradingStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nqd.nqd_lms_be.ai.AiGradingService;
import com.nqd.nqd_lms_be.ai.dto.AiGradingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAssignmentRepository examAssignmentRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ExamAttemptEventRepository examAttemptEventRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;
    private final com.nqd.nqd_lms_be.service.certificate.CertificateService certificateService;
    private final AiGradingService aiGradingService;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExamResponse> getAvailableExams() {
        return examRepository.findByStatus(ExamStatus.PUBLISHED).stream()
                .map(exam -> TeacherExamResponse.builder()
                        .id(exam.getId())
                        .courseId(exam.getCourse() != null ? exam.getCourse().getId() : null)
                        .subjectId(exam.getSubject() != null ? exam.getSubject().getId() : null)
                        .title(exam.getTitle())
                        .description(exam.getDescription())
                        .instructions(exam.getInstructions())
                        .durationMinutes(exam.getDurationMinutes())
                        .totalMarks(exam.getTotalMarks())
                        .passingMarks(exam.getPassingMarks())
                        .maxAttempts(exam.getMaxAttempts())
                        .status(exam.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAssignedExamResponse> getMyAssignedExams(UUID studentId) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        // 1. Get explicit exam assignments
        List<ExamAssignment> assignments = examAssignmentRepository.findByStudentId(studentId);
        Map<UUID, LocalDateTime> assignedAtMap = assignments.stream()
                .collect(Collectors.toMap(ea -> ea.getExam().getId(), ExamAssignment::getAssignedAt, (a, b) -> a));

        Set<UUID> explicitExamIds = assignedAtMap.keySet();

        // 2. Get enrolled courses of the student
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentId(studentId);
        Set<UUID> enrolledCourseIds = enrollments.stream()
                .map(ce -> ce.getCourse().getId())
                .collect(Collectors.toSet());

        // 3. Find all published exams
        List<Exam> publishedExams = examRepository.findByStatus(ExamStatus.PUBLISHED);

        // Filter exams: either explicitly assigned OR belonging to an enrolled course
        List<Exam> studentExams = publishedExams.stream()
                .filter(e -> explicitExamIds.contains(e.getId()) || (e.getCourse() != null && enrolledCourseIds.contains(e.getCourse().getId())))
                .collect(Collectors.toList());

        List<ExamAttempt> myAttempts = examAttemptRepository.findByStudentId(studentId);
        Map<UUID, List<ExamAttempt>> attemptsByExam = myAttempts.stream()
                .collect(Collectors.groupingBy(a -> a.getExam().getId()));

        return studentExams.stream().map(e -> {
            List<ExamAttempt> attempts = attemptsByExam.getOrDefault(e.getId(), Collections.emptyList());
            int attemptsTaken = attempts.size();
            BigDecimal bestScore = attempts.stream()
                    .map(ExamAttempt::getTotalScore)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
            boolean passed = attempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed()));
            long qCount = examQuestionRepository.countByExamId(e.getId());

            return StudentAssignedExamResponse.builder()
                    .examId(e.getId())
                    .code(e.getCode())
                    .title(e.getTitle())
                    .description(e.getDescription())
                    .instructions(e.getInstructions())
                    .subjectId(e.getSubject() != null ? e.getSubject().getId() : null)
                    .subjectName(e.getSubject() != null ? e.getSubject().getName() : null)
                    .gradeLevel(e.getGradeLevel())
                    .courseId(e.getCourse() != null ? e.getCourse().getId() : null)
                    .courseName(e.getCourse() != null ? e.getCourse().getName() : null)
                    .durationMinutes(e.getDurationMinutes())
                    .totalMarks(e.getTotalMarks())
                    .passingMarks(e.getPassingMarks())
                    .maxAttempts(e.getMaxAttempts())
                    .questionCount((int) qCount)
                    .status(e.getStatus())
                    .attemptsTaken(attemptsTaken)
                    .bestScore(bestScore)
                    .passed(passed)
                    .assignedAt(assignedAtMap.get(e.getId()))
                    .teacherName(e.getCreator() != null ? e.getCreator().getFullName() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentExamTakingResponse startExam(UUID examId, UUID studentId) {
        // Enforce Weekly Exam limit for Free Students
        membershipEntitlementService.enforceAndConsumeUsage(
                studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.EXAM_LIMIT, 1
        );

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ForbiddenOperationException("Exam is not published or available for taking");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        long existingAttempts = examAttemptRepository.countByExamIdAndStudentId(examId, studentId);
        if (exam.getMaxAttempts() != null && existingAttempts >= exam.getMaxAttempts()) {
            throw new ForbiddenOperationException("You have reached the maximum number of attempts for this exam");
        }

        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .student(student)
                .attemptNumber((int) existingAttempts + 1)
                .status(ExamAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        attempt = examAttemptRepository.save(attempt);

        // Fetch questions for taking (SANITZED - NO ANSWER KEYS)
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(examId);
        List<StudentQuestionTakingResponse> questionDtos = new ArrayList<>();

        for (ExamQuestion eq : examQuestions) {
            Question q = eq.getQuestion();
            boolean isChoiceType = q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE;
            List<QuestionOption> options = isChoiceType
                    ? questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId())
                    : Collections.emptyList();

            // Sanitize options - DO NOT expose isCorrect or ANS key
            List<StudentOptionTakingResponse> optionDtos = options.stream()
                    .filter(opt -> !"ANS".equalsIgnoreCase(opt.getOptionKey()))
                    .map(opt -> StudentOptionTakingResponse.builder()
                            .id(opt.getId())
                            .optionKey(opt.getOptionKey())
                            .optionText(opt.getOptionText())
                            .displayOrder(opt.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());

            questionDtos.add(StudentQuestionTakingResponse.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .marks(eq.getMarks())
                    .displayOrder(eq.getDisplayOrder())
                    .audioUrl(q.getAudioUrl())
                    .imageUrl(q.getImageUrl())
                    .options(optionDtos)
                    .build());
        }

        log.info("Student {} started exam attempt {} for exam {}", student.getEmail(), attempt.getId(), exam.getTitle());

        return StudentExamTakingResponse.builder()
                .examId(exam.getId())
                .attemptId(attempt.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .instructions(exam.getInstructions())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .passingMarks(exam.getPassingMarks())
                .audioUrl(exam.getAudioUrl())
                .maxListeningPlays(exam.getMaxListeningPlays())
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .enableProctoring(Boolean.TRUE.equals(exam.getEnableProctoring()))
                .maxViolationCount(exam.getMaxViolationCount() != null ? exam.getMaxViolationCount() : 5)
                .violationCount(attempt.getViolationCount() != null ? attempt.getViolationCount() : 0)
                .isFlagged(Boolean.TRUE.equals(attempt.getIsFlagged()))
                .questions(questionDtos)
                .build();
    }

    @Override
    @Transactional
    public StudentAttemptResultResponse submitExam(UUID attemptId, SubmitExamAttemptRequest request, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        // CRITICAL OWNERSHIP CHECK: Student can only submit their own attempt
        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("You cannot submit another student's exam attempt");
        }

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new ForbiddenOperationException("This exam attempt has already been submitted or completed");
        }

        Exam exam = attempt.getExam();
        BigDecimal totalScore = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        // Enforce time boundary with 5 min grace period
        int duration = exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 60;
        LocalDateTime maxAllowedTime = attempt.getStartedAt().plusMinutes(duration).plusMinutes(5);
        if (now.isAfter(maxAllowedTime)) {
            attempt.setIsFlagged(true);
            String reason = attempt.getFlagReason() != null ? attempt.getFlagReason() + "; Nộp bài trễ hơn thời gian cho phép" : "Nộp bài trễ hơn thời gian cho phép";
            attempt.setFlagReason(reason);
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId());
        Map<UUID, StudentAnswerSubmissionDto> submittedAnswerMap = new HashMap<>();
        if (request != null && request.getAnswers() != null) {
            for (StudentAnswerSubmissionDto ans : request.getAnswers()) {
                if (ans.getQuestionId() != null) {
                    submittedAnswerMap.put(ans.getQuestionId(), ans);
                }
            }
        }

        for (ExamQuestion eq : examQuestions) {
            Question question = eq.getQuestion();
            StudentAnswerSubmissionDto ansDto = submittedAnswerMap.get(question.getId());

            QuestionOption selectedOption = null;
            String answerText = ansDto != null ? ansDto.getAnswerText() : null;
            boolean isCorrect = false;
            BigDecimal maxMarks = eq.getMarks() != null ? eq.getMarks() : question.getDefaultMarks();
            BigDecimal marksAwarded = BigDecimal.ZERO;
            String aiFeedback = null;
            GradingMethod gradingMethod = GradingMethod.AUTO;
            GradingStatus gradingStatus = GradingStatus.AUTO_GRADED;

            if (ansDto != null) {
                if (ansDto.getSelectedOptionId() != null) {
                    selectedOption = questionOptionRepository.findById(ansDto.getSelectedOptionId()).orElse(null);
                    if (selectedOption != null && Boolean.TRUE.equals(selectedOption.getIsCorrect())) {
                        isCorrect = true;
                        marksAwarded = maxMarks;
                    }
                } else if (answerText != null && !answerText.trim().isEmpty()) {
                    // Non-choice: SHORT_ANSWER, FILL_IN_THE_BLANK, ESSAY -> Grade via AiGradingService
                    AiGradingResult gradingResult = aiGradingService.grade(question, answerText, maxMarks);
                    if (gradingResult != null) {
                        isCorrect = gradingResult.isCorrect();
                        marksAwarded = gradingResult.getMarksAwarded();
                        aiFeedback = gradingResult.getFeedback();
                        gradingMethod = GradingMethod.AI;
                        gradingStatus = GradingStatus.AI_GRADED;
                    }
                }
            }

            totalScore = totalScore.add(marksAwarded);

            ExamAnswer answer = ExamAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .answerText(answerText)
                    .isCorrect(isCorrect)
                    .maxMarks(maxMarks)
                    .marksAwarded(marksAwarded)
                    .gradingStatus(gradingStatus)
                    .gradingMethod(gradingMethod)
                    .aiFeedback(aiFeedback)
                    .answeredAt(ansDto != null ? now : null)
                    .gradedAt(now)
                    .build();

            examAnswerRepository.save(answer);
        }

        attempt.setSubmittedAt(now);
        attempt.setGradedAt(now);
        attempt.setTotalScore(totalScore);
        attempt.setStatus(ExamAttemptStatus.SUBMITTED);

        if (exam.getTotalMarks() != null && exam.getTotalMarks().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalScore.multiply(new BigDecimal("100"))
                    .divide(exam.getTotalMarks(), 2, RoundingMode.HALF_UP);
            attempt.setPercentage(percentage);
            attempt.setPassed(totalScore.compareTo(exam.getPassingMarks()) >= 0);
        } else {
            attempt.setPercentage(BigDecimal.ZERO);
            attempt.setPassed(false);
        }

        attempt = examAttemptRepository.save(attempt);
        log.info("Student {} submitted attempt {}. Score: {}", studentId, attemptId, totalScore);

        if (Boolean.TRUE.equals(attempt.getPassed()) && exam.getCourse() != null) {
            certificateService.checkAndAutoIssueCertificate(studentId, exam.getCourse().getId());
        }

        return mapToResultResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttemptResultResponse getAttemptResult(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        // CRITICAL OWNERSHIP CHECK: Student can only view their own attempt result
        if (!attempt.getStudent().getId().equals(studentId) && !SecurityUtils.isAdmin() && !SecurityUtils.isTeacher()) {
            throw new ForbiddenOperationException("You cannot view another student's exam attempt result");
        }

        return mapToResultResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgress(UUID studentId) {
        long enrolledCount = courseEnrollmentRepository.findByStudentId(studentId).size();
        List<ExamAttempt> attempts = examAttemptRepository.findByStudentId(studentId);

        long passedCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double avgScore = attempts.stream()
                .filter(a -> a.getTotalScore() != null)
                .mapToDouble(a -> a.getTotalScore().doubleValue())
                .average()
                .orElse(0.0);

        return StudentProgressResponse.builder()
                .studentId(studentId)
                .totalEnrolledCourses(enrolledCount)
                .completedCourses(0L)
                .totalExamsTaken(attempts.size())
                .passedExams(passedCount)
                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttemptResultResponse> getMyExamAttempts(UUID examId, UUID studentId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentIdOrderByAttemptNumberAsc(examId, studentId);
        return attempts.stream().map(this::mapToResultResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentExamAttemptReviewResponse getAttemptReview(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        // CRITICAL OWNERSHIP CHECK: Student can only review their own attempt
        if (!attempt.getStudent().getId().equals(studentId) && !SecurityUtils.isAdmin() && !SecurityUtils.isTeacher()) {
            throw new ForbiddenOperationException("You cannot review another student's exam attempt");
        }

        Exam exam = attempt.getExam();
        Long durationSec = null;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            durationSec = java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId());
        List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(attemptId);
        Map<UUID, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(ans -> ans.getQuestion().getId(), ans -> ans, (a1, a2) -> a1));

        List<StudentExamAnswerReviewResponse> answerDtos = new ArrayList<>();

        for (ExamQuestion eq : examQuestions) {
            Question q = eq.getQuestion();
            ExamAnswer ans = answerMap.get(q.getId());

            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
            List<TeacherQuestionOptionDto> optionDtos = options != null ? options.stream()
                    .map(opt -> TeacherQuestionOptionDto.builder()
                            .id(opt.getId())
                            .optionKey(opt.getOptionKey())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .displayOrder(opt.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList()) : Collections.emptyList();

            QuestionOption correctOpt = options != null ? options.stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .findFirst()
                    .orElse(null) : null;

            QuestionOption studentOpt = ans != null ? ans.getSelectedOption() : null;

            answerDtos.add(StudentExamAnswerReviewResponse.builder()
                    .questionId(q.getId())
                    .displayOrder(eq.getDisplayOrder())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .maxMarks(eq.getMarks() != null ? eq.getMarks() : q.getDefaultMarks())
                    .marksAwarded(ans != null ? ans.getMarksAwarded() : BigDecimal.ZERO)
                    .isCorrect(ans != null ? ans.getIsCorrect() : false)
                    .studentSelectedOptionId(studentOpt != null ? studentOpt.getId() : null)
                    .studentSelectedOptionKey(studentOpt != null ? studentOpt.getOptionKey() : null)
                    .studentSelectedOptionText(studentOpt != null ? studentOpt.getOptionText() : null)
                    .studentAnswerText(ans != null ? ans.getAnswerText() : null)
                    .correctOptionKey(correctOpt != null ? correctOpt.getOptionKey() : null)
                    .correctOptionText(correctOpt != null ? correctOpt.getOptionText() : null)
                    .explanation(q.getExplanation())
                    .audioUrl(q.getAudioUrl())
                    .audioScript(q.getAudioScript())
                    .imageUrl(q.getImageUrl())
                    .options(optionDtos)
                    .build());
        }

        return StudentExamAttemptReviewResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .examCode(exam.getCode())
                .audioUrl(exam.getAudioUrl())
                .audioScript(exam.getAudioScript())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .durationSeconds(durationSec)
                .totalScore(attempt.getTotalScore())
                .maxScore(exam.getTotalMarks())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .answers(answerDtos)
                .build();
    }

    
    @Override
    @Transactional
    public ExamAttemptEventBatchResponse recordAttemptEvents(UUID attemptId, List<ExamAttemptEventRequest> requests, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", attemptId));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("You cannot submit events for another student's exam attempt");
        }

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            return ExamAttemptEventBatchResponse.builder()
                    .attemptId(attempt.getId())
                    .violationCount(attempt.getViolationCount())
                    .maxViolationCount(attempt.getExam().getMaxViolationCount())
                    .isFlagged(attempt.getIsFlagged())
                    .flagReason(attempt.getFlagReason())
                    .isAutoSubmitted(true)
                    .message("Attempt is already completed or submitted")
                    .build();
        }

        Exam exam = attempt.getExam();
        int maxViolations = exam.getMaxViolationCount() != null ? exam.getMaxViolationCount() : 5;
        int currentViolations = attempt.getViolationCount() != null ? attempt.getViolationCount() : 0;

        if (requests != null && !requests.isEmpty()) {
            List<ExamAttemptEvent> eventsToSave = new ArrayList<>();
            for (ExamAttemptEventRequest req : requests) {
                if (req.getEventType() == null) continue;

                boolean isViol = req.getIsViolation() != null ? req.getIsViolation() :
                        (req.getEventType() != ExamAttemptEventType.RESUME && req.getEventType() != ExamAttemptEventType.OTHER);

                eventsToSave.add(ExamAttemptEvent.builder()
                        .attempt(attempt)
                        .eventType(req.getEventType())
                        .occurredAt(req.getOccurredAt() != null ? req.getOccurredAt() : LocalDateTime.now())
                        .metadata(req.getMetadata())
                        .isViolation(isViol)
                        .build());

                if (isViol) {
                    currentViolations++;
                }
            }
            examAttemptEventRepository.saveAll(eventsToSave);
        }

        attempt.setViolationCount(currentViolations);

        boolean autoSubmitted = false;
        if (currentViolations >= maxViolations) {
            attempt.setIsFlagged(true);
            String reason = "Vượt quá số lần cảnh báo gian lận cho phép (" + currentViolations + "/" + maxViolations + ")";
            attempt.setFlagReason(reason);

            if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
                attempt.setStatus(ExamAttemptStatus.SUBMITTED);
                attempt.setSubmittedAt(LocalDateTime.now());
                attempt.setGradedAt(LocalDateTime.now());
                if (attempt.getTotalScore() == null) {
                    attempt.setTotalScore(BigDecimal.ZERO);
                }
                if (attempt.getPercentage() == null) {
                    attempt.setPercentage(BigDecimal.ZERO);
                }
                attempt.setPassed(false);
                autoSubmitted = true;
                log.warn("Student {} exceeded max violations ({}/{}). ExamAttempt {} auto-submitted & flagged!",
                        studentId, currentViolations, maxViolations, attempt.getId());
            }
        }

        examAttemptRepository.save(attempt);

        return ExamAttemptEventBatchResponse.builder()
                .attemptId(attempt.getId())
                .violationCount(currentViolations)
                .maxViolationCount(maxViolations)
                .isFlagged(attempt.getIsFlagged())
                .flagReason(attempt.getFlagReason())
                .isAutoSubmitted(autoSubmitted)
                .message(autoSubmitted ? "Bài thi đã tự động kết thúc do vượt quá số lần vi phạm cho phép." : "Ghi nhận sự kiện thành công.")
                .build();
    }

    private StudentAttemptResultResponse mapToResultResponse(ExamAttempt attempt) {
        return StudentAttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .examTitle(attempt.getExam().getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .totalScore(attempt.getTotalScore())
                .maxScore(attempt.getExam().getTotalMarks())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .violationCount(attempt.getViolationCount() != null ? attempt.getViolationCount() : 0)
                .isFlagged(Boolean.TRUE.equals(attempt.getIsFlagged()))
                .flagReason(attempt.getFlagReason())
                .build();
    }
}
