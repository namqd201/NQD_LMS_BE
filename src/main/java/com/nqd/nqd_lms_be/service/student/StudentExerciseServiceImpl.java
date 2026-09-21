package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.ai.StudentAiContextAssembler;
import com.nqd.nqd_lms_be.ai.StudentAiTutorEngine;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.exercise.*;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorRequest;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.repository.*;
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
public class StudentExerciseServiceImpl implements StudentExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseAttemptAnswerRepository exerciseAttemptAnswerRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final UserRepository userRepository;
    private final StudentAiTutorEngine studentAiTutorEngine;
    private final com.nqd.nqd_lms_be.service.certificate.CertificateService certificateService;
    private final AiGradingService aiGradingService;

    @Override
    @Transactional(readOnly = true)
    public List<StudentExerciseSummaryResponse> getExercisesByLesson(UUID lessonId, UUID studentId) {
        List<Exercise> exercises = exerciseRepository.findByLessonIdAndStatusAndIsDeletedFalse(lessonId, ExerciseStatus.PUBLISHED);
        return exercises.stream().map(e -> mapToStudentSummary(e, studentId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentExerciseSummaryResponse getExerciseById(UUID exerciseId, UUID studentId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .filter(e -> e.getStatus() == ExerciseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại hoặc chưa được phát hành."));

        return mapToStudentSummary(exercise, studentId);
    }

    @Override
    @Transactional
    public StudentExerciseTakingResponse startExercise(UUID exerciseId, UUID studentId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .filter(e -> e.getStatus() == ExerciseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập không tồn tại hoặc chưa được phát hành."));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin học viên."));

        // Check active in-progress attempt to resume
        Optional<ExerciseAttempt> inProgressOpt = exerciseAttemptRepository
                .findFirstByExerciseIdAndStudentIdAndStatusOrderByStartedAtDesc(exerciseId, studentId, ExerciseAttemptStatus.IN_PROGRESS);

        ExerciseAttempt attempt;
        if (inProgressOpt.isPresent()) {
            attempt = inProgressOpt.get();
        } else {
            // Check 3 attempts cycle & 10-minute cooldown
            List<ExerciseAttempt> allAttempts = exerciseAttemptRepository.findByExerciseIdAndStudentIdOrderByAttemptNumberAsc(exerciseId, studentId);
            long completedCount = allAttempts.stream().filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED).count();

            if (completedCount > 0 && completedCount % 3 == 0) {
                // Find the latest completed attempt
                ExerciseAttempt lastAttempt = allAttempts.stream()
                        .filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED)
                        .max(Comparator.comparing(ExerciseAttempt::getAttemptNumber))
                        .orElse(null);

                if (lastAttempt != null && !Boolean.TRUE.equals(lastAttempt.getPassed()) && lastAttempt.getSubmittedAt() != null) {
                    LocalDateTime cooldownEndsAt = lastAttempt.getSubmittedAt().plusMinutes(10);
                    if (LocalDateTime.now().isBefore(cooldownEndsAt)) {
                        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), cooldownEndsAt).getSeconds();
                        long mins = remainingSeconds / 60;
                        long secs = remainingSeconds % 60;
                        throw new ForbiddenOperationException(String.format(
                                "Bạn đã làm sai 3 lần liên tiếp. Vui lòng đợi %02d phút %02d giây để được làm lại.",
                                mins, secs
                        ));
                    }
                }
            }

            long attemptCount = allAttempts.size();
            if (exercise.getMaxAttempts() != null && exercise.getMaxAttempts() > 0 && attemptCount >= exercise.getMaxAttempts()) {
                throw new ForbiddenOperationException("Bạn đã hết số lần làm bài tập này (Tối đa " + exercise.getMaxAttempts() + " lần).");
            }

            List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(exerciseId);
            BigDecimal maxScore = eqList.stream().map(ExerciseQuestion::getMarks).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            attempt = ExerciseAttempt.builder()
                    .exercise(exercise)
                    .student(student)
                    .attemptNumber((int) attemptCount + 1)
                    .status(ExerciseAttemptStatus.IN_PROGRESS)
                    .startedAt(LocalDateTime.now())
                    .totalQuestions(eqList.size())
                    .maxScore(maxScore)
                    .build();

            attempt = exerciseAttemptRepository.save(attempt);
        }

        return buildTakingResponse(exercise, attempt);
    }

    @Override
    @Transactional
    public StudentExerciseQuestionResultResponse submitQuestion(
            UUID attemptId,
            StudentExerciseSubmitQuestionRequest request,
            UUID studentId
    ) {
        ExerciseAttempt attempt = exerciseAttemptRepository.findById(attemptId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài."));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("Bạn không có quyền thao tác trên lượt làm này.");
        }

        if (attempt.getStatus() != ExerciseAttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Lượt làm bài này đã hoàn thành hoặc đã đóng.");
        }

        ExerciseQuestion eq = exerciseQuestionRepository.findByExerciseIdAndQuestionId(attempt.getExercise().getId(), request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi không thuộc bài tập này."));

        Question question = eq.getQuestion();
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(question.getId());

        QuestionOption selectedOption = null;
        if (request.getSelectedOptionId() != null) {
            selectedOption = options.stream().filter(o -> o.getId().equals(request.getSelectedOptionId())).findFirst().orElse(null);
        }

        boolean isCorrect = false;
        BigDecimal marksAwarded = BigDecimal.ZERO;
        String aiExplanation = null;

        if (selectedOption != null && Boolean.TRUE.equals(selectedOption.getIsCorrect())) {
            isCorrect = true;
            marksAwarded = eq.getMarks() != null ? eq.getMarks() : BigDecimal.ONE;
        } else if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE || question.getQuestionType() == QuestionType.TRUE_FALSE) {
            isCorrect = false;
            marksAwarded = BigDecimal.ZERO;
        } else {
            // Non-choice types: SHORT_ANSWER, FILL_IN_THE_BLANK, ESSAY -> Grade using AiGradingService
            AiGradingResult gradingResult = aiGradingService.grade(question, request.getAnswerText(), eq.getMarks());
            if (gradingResult != null) {
                isCorrect = gradingResult.isCorrect();
                marksAwarded = gradingResult.getMarksAwarded();
                aiExplanation = gradingResult.getFeedback();
            }
        }

        // Upsert answer
        ExerciseAttemptAnswer answer = exerciseAttemptAnswerRepository
                .findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                .orElse(ExerciseAttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .maxMarks(eq.getMarks())
                        .build());

        answer.setSelectedOption(selectedOption);
        answer.setAnswerText(request.getAnswerText());
        answer.setIsCorrect(isCorrect);
        answer.setMarksAwarded(marksAwarded);
        answer.setAiExplanation(aiExplanation);
        answer.setAnsweredAt(LocalDateTime.now());

        exerciseAttemptAnswerRepository.save(answer);

        // REQUIREMENT: DO NOT REVEAL isCorrect, correctOptionId, or explanation during in-progress answering
        return StudentExerciseQuestionResultResponse.builder()
                .questionId(question.getId())
                .selectedOptionId(selectedOption != null ? selectedOption.getId() : null)
                .selectedOptionKey(selectedOption != null ? selectedOption.getOptionKey() : null)
                .answerText(request.getAnswerText())
                .isCorrect(null)
                .correctOptionId(null)
                .correctOptionKey(null)
                .explanation(null)
                .marksAwarded(null)
                .maxMarks(eq.getMarks())
                .aiExplanation(null)
                .answeredAt(answer.getAnsweredAt())
                .build();
    }

    @Override
    @Transactional
    public StudentExerciseAttemptResultResponse submitAttempt(UUID attemptId, UUID studentId) {
        ExerciseAttempt attempt = exerciseAttemptRepository.findById(attemptId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài."));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("Bạn không có quyền thao tác trên lượt làm này.");
        }

        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(attempt.getExercise().getId());
        List<ExerciseAttemptAnswer> answers = exerciseAttemptAnswerRepository.findByAttemptId(attemptId);
        Map<UUID, ExerciseAttemptAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a1, a2) -> a1));

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int correctCount = 0;

        for (ExerciseQuestion eq : eqList) {
            maxScore = maxScore.add(eq.getMarks() != null ? eq.getMarks() : BigDecimal.ZERO);
            ExerciseAttemptAnswer ans = answerMap.get(eq.getQuestion().getId());
            if (ans != null) {
                if (ans.getMarksAwarded() != null && ans.getMarksAwarded().compareTo(BigDecimal.ZERO) > 0) {
                    totalScore = totalScore.add(ans.getMarksAwarded());
                }
                if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                    correctCount++;
                }
            }
        }

        BigDecimal percentage = BigDecimal.ZERO;
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
            percentage = totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP);
        }

        // REQUIREMENT: Must achieve 100% correct to pass the exercise and advance to next lesson
        boolean passed = (eqList.size() > 0 && correctCount == eqList.size());

        attempt.setStatus(ExerciseAttemptStatus.COMPLETED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTotalScore(totalScore);
        attempt.setMaxScore(maxScore);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(eqList.size());

        exerciseAttemptRepository.save(attempt);

        if (passed && attempt.getExercise().getLesson() != null &&
                attempt.getExercise().getLesson().getChapter() != null &&
                attempt.getExercise().getLesson().getChapter().getCourse() != null) {
            UUID courseId = attempt.getExercise().getLesson().getChapter().getCourse().getId();
            certificateService.checkAndAutoIssueCertificate(studentId, courseId);
        }

        return mapToAttemptResultResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentExerciseAttemptResultResponse getAttemptResult(UUID attemptId, UUID studentId) {
        ExerciseAttempt attempt = exerciseAttemptRepository.findById(attemptId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài."));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("Bạn không có quyền xem kết quả này.");
        }

        return mapToAttemptResultResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExerciseAttemptResultResponse> getMyAttempts(UUID exerciseId, UUID studentId) {
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findByExerciseIdAndStudentIdOrderByAttemptNumberDesc(exerciseId, studentId);
        return attempts.stream().map(this::mapToAttemptResultResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentExerciseAiExplainResponse explainQuestionWithAi(
            UUID attemptId,
            UUID questionId,
            String customPrompt,
            UUID studentId
    ) {
        ExerciseAttempt attempt = exerciseAttemptRepository.findById(attemptId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài."));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new ForbiddenOperationException("Bạn không có quyền thao tác trên lượt làm này.");
        }

        if (attempt.getStatus() != ExerciseAttemptStatus.COMPLETED) {
            throw new ForbiddenOperationException("Bạn chỉ có thể xem AI giải thích chi tiết sau khi đã nộp bài tập.");
        }

        ExerciseQuestion eq = exerciseQuestionRepository.findByExerciseIdAndQuestionId(attempt.getExercise().getId(), questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi không thuộc bài tập này."));

        Question question = eq.getQuestion();
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(question.getId());
        QuestionOption correctOption = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);

        ExerciseAttemptAnswer answer = exerciseAttemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId).orElse(null);
        String studentSelectionText = "Chưa chọn đáp án";
        if (answer != null && answer.getSelectedOption() != null) {
            studentSelectionText = answer.getSelectedOption().getOptionKey() + ": " + answer.getSelectedOption().getOptionText();
        }

        String correctOptionText = correctOption != null ? (correctOption.getOptionKey() + ": " + correctOption.getOptionText()) : "Không xác định";

        String prompt = customPrompt != null && !customPrompt.isBlank()
                ? customPrompt
                : "Hãy giải thích chi tiết phương pháp giải câu hỏi bài tập này, vì sao đáp án " + (correctOption != null ? correctOption.getOptionKey() : "") + " là đúng và các bẫy thường gặp.";

        // Build context for AI Tutor Engine
        StudentAiContextAssembler.AssembledStudentContext context = StudentAiContextAssembler.AssembledStudentContext.builder()
                .courseTitle(attempt.getExercise().getLesson().getChapter().getCourse().getName())
                .lessonTitle(attempt.getExercise().getLesson().getTitle())
                .questionContent(question.getContent())
                .isExamInProgress(false)
                .formattedContext(String.format("""
                        Khóa học: %s
                        Bài học: %s
                        Bài tập: %s
                        Đề bài: %s
                        Các lựa chọn:
                        %s
                        Đáp án đúng: %s
                        Lựa chọn của học sinh: %s
                        Giải thích sẵn có: %s
                        """,
                        attempt.getExercise().getLesson().getChapter().getCourse().getName(),
                        attempt.getExercise().getLesson().getTitle(),
                        attempt.getExercise().getTitle(),
                        question.getContent(),
                        options.stream().map(o -> o.getOptionKey() + ". " + o.getOptionText()).collect(Collectors.joining("\n")),
                        correctOptionText,
                        studentSelectionText,
                        question.getExplanation() != null ? question.getExplanation() : "Không có"
                ))
                .build();

        StudentAiTutorRequest aiReq = StudentAiTutorRequest.builder()
                .question(prompt)
                .mode(StudentAiTutorMode.EXPLAIN_WRONG_ANSWER)
                .build();

        StudentAiTutorResponse aiRes = studentAiTutorEngine.generateTutorResponse(aiReq, context, 999, studentId, "STUDENT");
        String aiAnswer = aiRes != null && aiRes.getAnswer() != null ? aiRes.getAnswer() : "Đang kết nối tới AI Tutor...";

        if (answer != null) {
            answer.setAiExplanation(aiAnswer);
            exerciseAttemptAnswerRepository.save(answer);
        }

        return StudentExerciseAiExplainResponse.builder()
                .questionId(questionId)
                .questionContent(question.getContent())
                .studentAnswer(studentSelectionText)
                .correctAnswer(correctOptionText)
                .explanation(aiAnswer)
                .build();
    }

    private record AttemptCycleInfo(Long cooldownRemainingSeconds, Integer attemptCycleCount, Integer maxCycleAttempts) {}

    private AttemptCycleInfo computeAttemptCycleInfo(List<ExerciseAttempt> userAttempts) {
        long completedCount = userAttempts.stream().filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED).count();
        boolean hasPassed = userAttempts.stream().anyMatch(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED && Boolean.TRUE.equals(a.getPassed()));

        int cycleCount = (int) (completedCount % 3);
        Long cooldownRemaining = 0L;

        if (completedCount > 0 && cycleCount == 0 && !hasPassed) {
            cycleCount = 3;
            ExerciseAttempt lastAttempt = userAttempts.stream()
                    .filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED)
                    .max(Comparator.comparing(ExerciseAttempt::getAttemptNumber))
                    .orElse(null);

            if (lastAttempt != null && lastAttempt.getSubmittedAt() != null) {
                LocalDateTime cooldownEndsAt = lastAttempt.getSubmittedAt().plusMinutes(10);
                if (LocalDateTime.now().isBefore(cooldownEndsAt)) {
                    cooldownRemaining = java.time.Duration.between(LocalDateTime.now(), cooldownEndsAt).getSeconds();
                } else {
                    cycleCount = 0;
                }
            }
        }

        return new AttemptCycleInfo(cooldownRemaining, cycleCount, 3);
    }

    private StudentExerciseSummaryResponse mapToStudentSummary(Exercise e, UUID studentId) {
        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(e.getId());
        BigDecimal totalMarks = eqList.stream().map(ExerciseQuestion::getMarks).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ExerciseAttempt> userAttempts = exerciseAttemptRepository.findByExerciseIdAndStudentIdOrderByAttemptNumberDesc(e.getId(), studentId);
        BigDecimal bestScore = userAttempts.stream()
                .filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED)
                .map(ExerciseAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal bestPct = userAttempts.stream()
                .filter(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED)
                .map(ExerciseAttempt::getPercentage)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        boolean passed = userAttempts.stream()
                .anyMatch(a -> a.getStatus() == ExerciseAttemptStatus.COMPLETED && Boolean.TRUE.equals(a.getPassed()));

        Optional<ExerciseAttempt> inProgress = userAttempts.stream()
                .filter(a -> a.getStatus() == ExerciseAttemptStatus.IN_PROGRESS)
                .findFirst();

        AttemptCycleInfo cycleInfo = computeAttemptCycleInfo(userAttempts);

        return StudentExerciseSummaryResponse.builder()
                .id(e.getId())
                .lessonId(e.getLesson() != null ? e.getLesson().getId() : null)
                .lessonTitle(e.getLesson() != null ? e.getLesson().getTitle() : null)
                .title(e.getTitle())
                .description(e.getDescription())
                .instructions(e.getInstructions())
                .type(e.getType())
                .timeLimitMinutes(e.getTimeLimitMinutes())
                .passingScore(e.getPassingScore())
                .questionCount(eqList.size())
                .totalMarks(totalMarks)
                .maxAttempts(e.getMaxAttempts())
                .showExplanationImmediately(e.getShowExplanationImmediately())
                .allowRetry(e.getAllowRetry())
                .userAttemptsCount((long) userAttempts.size())
                .userBestScore(bestScore)
                .userBestPercentage(bestPct)
                .userPassed(passed)
                .hasInProgressAttempt(inProgress.isPresent())
                .inProgressAttemptId(inProgress.map(ExerciseAttempt::getId).orElse(null))
                .cooldownRemainingSeconds(cycleInfo.cooldownRemainingSeconds())
                .attemptCycleCount(cycleInfo.attemptCycleCount())
                .maxCycleAttempts(cycleInfo.maxCycleAttempts())
                .build();
    }

    private StudentExerciseTakingResponse buildTakingResponse(Exercise exercise, ExerciseAttempt attempt) {
        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(exercise.getId());
        BigDecimal totalMarks = eqList.stream().map(ExerciseQuestion::getMarks).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StudentExerciseQuestionTakingResponse> questions = eqList.stream().map(eq -> {
            Question q = eq.getQuestion();
            boolean isChoiceType = q.getQuestionType() == QuestionType.MULTIPLE_CHOICE || q.getQuestionType() == QuestionType.TRUE_FALSE;
            List<QuestionOption> options = isChoiceType
                    ? questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId())
                    : Collections.emptyList();

            List<StudentExerciseOptionTakingResponse> optTaking = options.stream()
                    .filter(opt -> !"ANS".equalsIgnoreCase(opt.getOptionKey()))
                    .map(opt ->
                            StudentExerciseOptionTakingResponse.builder()
                                    .id(opt.getId())
                                    .optionKey(opt.getOptionKey())
                                    .optionText(opt.getOptionText())
                                    .displayOrder(opt.getDisplayOrder())
                                    .build()
                    ).collect(Collectors.toList());

            return StudentExerciseQuestionTakingResponse.builder()
                    .questionId(q.getId())
                    .displayOrder(eq.getDisplayOrder())
                    .marks(eq.getMarks())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .options(optTaking)
                    .build();
        }).collect(Collectors.toList());

        List<ExerciseAttemptAnswer> answers = exerciseAttemptAnswerRepository.findByAttemptId(attempt.getId());
        boolean isCompleted = attempt.getStatus() == ExerciseAttemptStatus.COMPLETED;
        List<StudentExerciseQuestionResultResponse> answered = answers.stream().map(ans -> {
            Question q = ans.getQuestion();
            List<QuestionOption> options = isCompleted
                    ? questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId())
                    : Collections.emptyList();
            QuestionOption correctOpt = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);

            return StudentExerciseQuestionResultResponse.builder()
                    .questionId(q.getId())
                    .selectedOptionId(ans.getSelectedOption() != null ? ans.getSelectedOption().getId() : null)
                    .selectedOptionKey(ans.getSelectedOption() != null ? ans.getSelectedOption().getOptionKey() : null)
                    .answerText(ans.getAnswerText())
                    .isCorrect(isCompleted ? ans.getIsCorrect() : null)
                    .correctOptionId(isCompleted && correctOpt != null ? correctOpt.getId() : null)
                    .correctOptionKey(isCompleted && correctOpt != null ? correctOpt.getOptionKey() : null)
                    .explanation(isCompleted ? q.getExplanation() : null)
                    .marksAwarded(isCompleted ? ans.getMarksAwarded() : null)
                    .maxMarks(ans.getMaxMarks())
                    .aiExplanation(isCompleted ? ans.getAiExplanation() : null)
                    .answeredAt(ans.getAnsweredAt())
                    .build();
        }).collect(Collectors.toList());

        List<ExerciseAttempt> userAttempts = exerciseAttemptRepository.findByExerciseIdAndStudentIdOrderByAttemptNumberDesc(exercise.getId(), attempt.getStudent().getId());
        AttemptCycleInfo cycleInfo = computeAttemptCycleInfo(userAttempts);

        return StudentExerciseTakingResponse.builder()
                .exerciseId(exercise.getId())
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .title(exercise.getTitle())
                .description(exercise.getDescription())
                .instructions(exercise.getInstructions())
                .type(exercise.getType())
                .timeLimitMinutes(exercise.getTimeLimitMinutes())
                .passingScore(exercise.getPassingScore())
                .showExplanationImmediately(exercise.getShowExplanationImmediately())
                .allowRetry(exercise.getAllowRetry())
                .startedAt(attempt.getStartedAt())
                .totalQuestions(eqList.size())
                .totalMarks(totalMarks)
                .questions(questions)
                .answeredQuestions(answered)
                .cooldownRemainingSeconds(cycleInfo.cooldownRemainingSeconds())
                .attemptCycleCount(cycleInfo.attemptCycleCount())
                .maxCycleAttempts(cycleInfo.maxCycleAttempts())
                .build();
    }

    private StudentExerciseAttemptResultResponse mapToAttemptResultResponse(ExerciseAttempt attempt) {
        Exercise exercise = attempt.getExercise();
        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(exercise.getId());
        List<ExerciseAttemptAnswer> answers = exerciseAttemptAnswerRepository.findByAttemptId(attempt.getId());
        Map<UUID, ExerciseAttemptAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a1, a2) -> a1));

        List<StudentExerciseQuestionResultResponse> questionResults = eqList.stream().map(eq -> {
            Question q = eq.getQuestion();
            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
            QuestionOption correctOpt = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);
            ExerciseAttemptAnswer ans = answerMap.get(q.getId());

            return StudentExerciseQuestionResultResponse.builder()
                    .questionId(q.getId())
                    .selectedOptionId(ans != null && ans.getSelectedOption() != null ? ans.getSelectedOption().getId() : null)
                    .selectedOptionKey(ans != null && ans.getSelectedOption() != null ? ans.getSelectedOption().getOptionKey() : null)
                    .answerText(ans != null ? ans.getAnswerText() : null)
                    .isCorrect(ans != null ? ans.getIsCorrect() : false)
                    .correctOptionId(correctOpt != null ? correctOpt.getId() : null)
                    .correctOptionKey(correctOpt != null ? correctOpt.getOptionKey() : null)
                    .explanation(q.getExplanation())
                    .marksAwarded(ans != null ? ans.getMarksAwarded() : BigDecimal.ZERO)
                    .maxMarks(eq.getMarks())
                    .aiExplanation(ans != null ? ans.getAiExplanation() : null)
                    .answeredAt(ans != null ? ans.getAnsweredAt() : null)
                    .build();
        }).collect(Collectors.toList());

        List<ExerciseAttempt> userAttempts = exerciseAttemptRepository.findByExerciseIdAndStudentIdOrderByAttemptNumberDesc(exercise.getId(), attempt.getStudent().getId());
        AttemptCycleInfo cycleInfo = computeAttemptCycleInfo(userAttempts);

        long totalAttempts = userAttempts.size();
        boolean canRetry = Boolean.TRUE.equals(exercise.getAllowRetry());
        Integer remainingAttempts = null;
        if (exercise.getMaxAttempts() != null && exercise.getMaxAttempts() > 0) {
            remainingAttempts = Math.max(0, (int) (exercise.getMaxAttempts() - totalAttempts));
            if (remainingAttempts <= 0) {
                canRetry = false;
            }
        }

        if (cycleInfo.cooldownRemainingSeconds() > 0) {
            canRetry = false;
        }

        return StudentExerciseAttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .exerciseId(exercise.getId())
                .exerciseTitle(exercise.getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .totalScore(attempt.getTotalScore())
                .maxScore(attempt.getMaxScore())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .correctCount(attempt.getCorrectCount())
                .totalQuestions(attempt.getTotalQuestions())
                .canRetry(canRetry)
                .remainingAttempts(remainingAttempts)
                .cooldownRemainingSeconds(cycleInfo.cooldownRemainingSeconds())
                .attemptCycleCount(cycleInfo.attemptCycleCount())
                .maxCycleAttempts(cycleInfo.maxCycleAttempts())
                .questionResults(questionResults)
                .build();
    }
}
