package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.ai.AIProvider;
import com.nqd.nqd_lms_be.ai.AIProviderFactory;
import com.nqd.nqd_lms_be.ai.dto.AiQuestionGenerationPrompt;
import com.nqd.nqd_lms_be.ai.dto.ExamBlueprintItem;
import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;
import com.nqd.nqd_lms_be.ai.validator.AiQuestionValidator;
import com.nqd.nqd_lms_be.ai.audio.AiAudioService;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherAiGenerationServiceImpl implements TeacherAiGenerationService {

    private final AIProviderFactory aiProviderFactory;
    private final AiQuestionValidator aiQuestionValidator;

    private final AiGenerationJobRepository aiGenerationJobRepository;
    private final AiGeneratedQuestionRepository aiGeneratedQuestionRepository;
    private final AiGeneratedOptionRepository aiGeneratedOptionRepository;

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;
    private final TransactionTemplate transactionTemplate;
    private final AiAudioService aiAudioService;

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public TeacherAiJobDetailResponse generateQuestions(TeacherAiGenerateQuestionsRequest request, UUID teacherId) {
        // Enforce AI generation feature entitlement
        membershipEntitlementService.enforceFeatureAccess(
                teacherId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.AI_EXAM_GENERATION
        );

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
            validateCourseTeacher(course, teacherId);
        }

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()));
        }

        String grade = request.getGradeLevel();
        if ((grade == null || grade.isBlank()) && course != null) {
            grade = course.getGradeLevel();
        }

        QuestionCategory resolvedCategory = null;
        if (request.getCategoryId() != null) {
            resolvedCategory = questionCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", request.getCategoryId()));
        } else {
            if (grade != null && !grade.isBlank()) {
                resolvedCategory = questionCategoryRepository
                        .findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
                                subject.getId(), grade, QuestionCategoryVisibility.PUBLIC
                        ).orElse(null);
            }
            if (resolvedCategory == null) {
                resolvedCategory = questionCategoryRepository
                        .findBySubjectIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(subject.getId())
                        .stream()
                        .filter(c -> c.getVisibility() == QuestionCategoryVisibility.PUBLIC)
                        .findFirst()
                        .orElse(null);
            }
        }

        final Course targetCourse = course;
        final Lesson targetLesson = lesson;
        final String targetGrade = grade;
        final QuestionCategory targetCategory = resolvedCategory;

        // Detect if this is a listening test
        boolean isListening = Boolean.TRUE.equals(request.getIsListening())
                || (request.getTopic() != null && request.getTopic().toLowerCase().matches(".*(nghe|listening|comprehension).*"))
                || (subject.getName().toLowerCase().contains("tiếng anh") && request.getAdditionalInstructions() != null && request.getAdditionalInstructions().toLowerCase().contains("nghe"));

        // 1. Create Job in DB (Short transaction)
        AiGenerationJob job = transactionTemplate.execute(status ->
                createInitialQuestionJob(teacher, subject, targetCourse, targetLesson, targetGrade, targetCategory, request)
        );

        // 2. Build Prompt
        AiQuestionGenerationPrompt prompt = AiQuestionGenerationPrompt.builder()
                .subjectName(subject.getName())
                .courseName(course != null ? course.getName() : null)
                .lessonName(lesson != null ? lesson.getTitle() : null)
                .gradeLevel(grade)
                .topic(request.getTopic())
                .questionType(request.getQuestionType())
                .questionTypes(request.getQuestionTypes())
                .difficulty(request.getDifficulty())
                .count(request.getNumberOfQuestions())
                .marksPerQuestion(request.getMarksPerQuestion())
                .additionalInstructions(request.getAdditionalInstructions())
                .isExamBlueprint(false)
                .isListening(isListening)
                .listeningPassageType(request.getListeningPassageType())
                .build();

        // 3. Invoke AI Provider (OUTSIDE of @Transactional to prevent DB connection holding)
        AIProvider provider = aiProviderFactory.getProvider();
        log.info("Generating questions for teacher {} using AI Provider: {} (isListening: {})",
                teacherId, provider.getProviderName(), isListening);

        List<GeneratedQuestionDraft> drafts = provider.generateQuestions(prompt);

        // Synthesize audio for listening drafts if present
        synthesizeAudioForDrafts(drafts);

        // 4. Save generated drafts to DB (Short transaction)
        return transactionTemplate.execute(status ->
                saveJobDraftResults(job.getId(), drafts)
        );
    }

    @Transactional
    public AiGenerationJob createInitialQuestionJob(
            User teacher,
            Subject subject,
            Course course,
            Lesson lesson,
            String grade,
            QuestionCategory category,
            TeacherAiGenerateQuestionsRequest request
    ) {
        AiGenerationJob job = AiGenerationJob.builder()
                .creator(teacher)
                .jobType(AiJobType.QUESTION_GENERATION)
                .status(AiJobStatus.PROCESSING)
                .subject(subject)
                .course(course)
                .lesson(lesson)
                .gradeLevel(grade)
                .category(category)
                .topic(request.getTopic())
                .totalRequested(request.getNumberOfQuestions() != null ? request.getNumberOfQuestions() : 5)
                .promptSummary(String.format("Tạo %d câu hỏi môn %s (Chủ đề: %s)",
                        request.getNumberOfQuestions(), subject.getName(), request.getTopic()))
                .build();

        return aiGenerationJobRepository.save(job);
    }

    @Override
    public TeacherAiJobDetailResponse generateExam(TeacherAiGenerateExamRequest request, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
            validateCourseTeacher(course, teacherId);
        }

        String grade = request.getGradeLevel();
        if ((grade == null || grade.isBlank()) && course != null) {
            grade = course.getGradeLevel();
        }

        String examCode = generateUniqueExamCode(grade, subject.getCode());

        int totalRequested = request.getBlueprintItems() != null
                ? request.getBlueprintItems().stream().mapToInt(bp -> bp.getCount() != null ? bp.getCount() : 1).sum()
                : 10;

        final Course targetCourse = course;
        final String targetGrade = grade;

        // 1. Create Job in DB (Short transaction)
        AiGenerationJob job = transactionTemplate.execute(status ->
                createInitialExamJob(teacher, subject, targetCourse, targetGrade, examCode, totalRequested, request)
        );

        // 2. Build Prompt with Blueprint Items
        List<ExamBlueprintItem> bpItems = new ArrayList<>();
        if (request.getBlueprintItems() != null) {
            for (ExamBlueprintItemRequest reqBp : request.getBlueprintItems()) {
                bpItems.add(ExamBlueprintItem.builder()
                        .topic(reqBp.getTopic() != null ? reqBp.getTopic() : request.getTopic())
                        .questionType(reqBp.getQuestionType() != null ? reqBp.getQuestionType() : QuestionType.MULTIPLE_CHOICE)
                        .difficulty(reqBp.getDifficulty() != null ? reqBp.getDifficulty() : QuestionDifficulty.MEDIUM)
                        .count(reqBp.getCount() != null ? reqBp.getCount() : 1)
                        .marksPerQuestion(reqBp.getMarksPerQuestion() != null ? reqBp.getMarksPerQuestion() : BigDecimal.ONE)
                        .build());
            }
        }

        boolean isListening = Boolean.TRUE.equals(request.getIsListening())
                || (request.getTitle() != null && request.getTitle().toLowerCase().matches(".*(nghe|listening).*"))
                || (request.getTopic() != null && request.getTopic().toLowerCase().matches(".*(nghe|listening).*"))
                || (subject.getName().toLowerCase().contains("tiếng anh") && request.getAdditionalInstructions() != null && request.getAdditionalInstructions().toLowerCase().contains("nghe"));

        AiQuestionGenerationPrompt prompt = AiQuestionGenerationPrompt.builder()
                .subjectName(subject.getName())
                .courseName(course != null ? course.getName() : null)
                .gradeLevel(grade)
                .topic(request.getTopic() != null ? request.getTopic() : request.getTitle())
                .count(totalRequested)
                .additionalInstructions(request.getAdditionalInstructions())
                .blueprintItems(bpItems)
                .isExamBlueprint(true)
                .isListening(isListening)
                .listeningPassageType(request.getListeningPassageType())
                .build();

        // 3. Invoke AI Provider (OUTSIDE of @Transactional to prevent DB connection holding)
        AIProvider provider = aiProviderFactory.getProvider();
        log.info("Generating exam blueprint for teacher {} using AI Provider: {} (isListening: {})",
                teacherId, provider.getProviderName(), isListening);

        List<GeneratedQuestionDraft> drafts = provider.generateQuestions(prompt);

        // Synthesize audio for listening drafts if present
        synthesizeAudioForDrafts(drafts);

        // 4. Save generated drafts to DB (Short transaction)
        return transactionTemplate.execute(status ->
                saveJobDraftResults(job.getId(), drafts)
        );
    }

    @Transactional
    public AiGenerationJob createInitialExamJob(
            User teacher,
            Subject subject,
            Course course,
            String grade,
            String examCode,
            int totalRequested,
            TeacherAiGenerateExamRequest request
    ) {
        AiGenerationJob job = AiGenerationJob.builder()
                .creator(teacher)
                .jobType(AiJobType.EXAM_GENERATION)
                .status(AiJobStatus.PROCESSING)
                .subject(subject)
                .course(course)
                .gradeLevel(grade)
                .topic(request.getTopic() != null ? request.getTopic() : request.getTitle())
                .targetExamTitle(request.getTitle())
                .targetExamCode(examCode)
                .targetExamDuration(request.getDurationMinutes())
                .targetExamPassingMarks(request.getPassingMarks())
                .targetExamTotalMarks(request.getTotalMarks())
                .totalRequested(totalRequested)
                .promptSummary(String.format("Biên soạn đề thi AI: %s (%s, %d phút)",
                        request.getTitle(), subject.getName(), request.getDurationMinutes()))
                .build();

        return aiGenerationJobRepository.save(job);
    }

    @Transactional
    public TeacherAiJobDetailResponse saveJobDraftResults(UUID jobId, List<GeneratedQuestionDraft> drafts) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));

        List<AiGeneratedQuestion> generatedQuestions = new ArrayList<>();
        int order = 1;

        for (GeneratedQuestionDraft draft : drafts) {
            AiQuestionValidator.ValidationResult valRes = aiQuestionValidator.validate(draft);

            AiGeneratedQuestion genQ = AiGeneratedQuestion.builder()
                    .job(job)
                    .content(draft.getContent())
                    .audioUrl(draft.getAudioUrl())
                    .audioScript(draft.getAudioScript())
                    .questionType(draft.getQuestionType())
                    .difficulty(draft.getDifficulty())
                    .marks(draft.getDefaultMarks() != null ? draft.getDefaultMarks() : BigDecimal.ONE)
                    .explanation(draft.getExplanation())
                    .tags(draft.getTags())
                    .displayOrder(order++)
                    .validationStatus(valRes.status())
                    .validationFeedback(valRes.feedback())
                    .reviewStatus(AiReviewStatus.PENDING_REVIEW)
                    .build();

            genQ = aiGeneratedQuestionRepository.save(genQ);

            if (draft.getOptions() != null) {
                int optOrder = 1;
                for (GeneratedOptionDraft optDraft : draft.getOptions()) {
                    AiGeneratedOption opt = AiGeneratedOption.builder()
                            .generatedQuestion(genQ)
                            .optionKey(optDraft.getOptionKey())
                            .optionText(optDraft.getOptionText())
                            .isCorrect(Boolean.TRUE.equals(optDraft.getIsCorrect()))
                            .displayOrder(optDraft.getDisplayOrder() != null ? optDraft.getDisplayOrder() : optOrder++)
                            .build();
                    aiGeneratedOptionRepository.save(opt);
                    genQ.getOptions().add(opt);
                }
            }

            generatedQuestions.add(genQ);
        }

        job.setStatus(AiJobStatus.COMPLETED);
        job.setTotalGenerated(generatedQuestions.size());
        if (job.getGeneratedQuestions() != null) {
            job.getGeneratedQuestions().clear();
            job.getGeneratedQuestions().addAll(generatedQuestions);
        }
        job = aiGenerationJobRepository.save(job);

        return mapToJobDetailResponse(job, generatedQuestions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAiJobResponse> getMyJobs(UUID teacherId) {
        List<AiGenerationJob> jobs = aiGenerationJobRepository.findByCreatorIdOrderByCreatedAtDesc(teacherId);
        return jobs.stream().map(this::mapToJobResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAiJobDetailResponse getJobDetail(UUID jobId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        List<AiGeneratedQuestion> questions = aiGeneratedQuestionRepository.findByJobIdOrderByDisplayOrderAsc(jobId);
        return mapToJobDetailResponse(job, questions);
    }

    @Override
    @Transactional
    public TeacherAiGeneratedQuestionResponse updateGeneratedQuestion(
            UUID jobId,
            UUID questionId,
            TeacherAiUpdateGeneratedQuestionRequest request,
            UUID teacherId
    ) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        AiGeneratedQuestion question = aiGeneratedQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGeneratedQuestion", questionId));

        if (!question.getJob().getId().equals(jobId)) {
            throw new ForbiddenOperationException("Question does not belong to the specified AI job");
        }

        question.setContent(request.getContent());
        if (request.getAudioUrl() != null) {
            question.setAudioUrl(request.getAudioUrl());
        }
        if (request.getAudioScript() != null) {
            question.setAudioScript(request.getAudioScript());
        }
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks() != null ? request.getMarks() : BigDecimal.ONE);
        question.setExplanation(request.getExplanation());
        question.setTags(request.getTags());

        // Replace options
        question.getOptions().clear();
        aiGeneratedOptionRepository.deleteAll(aiGeneratedOptionRepository.findByGeneratedQuestionIdOrderByDisplayOrderAsc(questionId));

        if (request.getOptions() != null) {
            int optOrder = 1;
            for (GeneratedOptionDraft optDraft : request.getOptions()) {
                AiGeneratedOption opt = AiGeneratedOption.builder()
                        .generatedQuestion(question)
                        .optionKey(optDraft.getOptionKey())
                        .optionText(optDraft.getOptionText())
                        .isCorrect(Boolean.TRUE.equals(optDraft.getIsCorrect()))
                        .displayOrder(optDraft.getDisplayOrder() != null ? optDraft.getDisplayOrder() : optOrder++)
                        .build();
                aiGeneratedOptionRepository.save(opt);
                question.getOptions().add(opt);
            }
        }

        // Re-validate updated question
        GeneratedQuestionDraft draft = GeneratedQuestionDraft.builder()
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .defaultMarks(question.getMarks())
                .explanation(question.getExplanation())
                .tags(question.getTags())
                .options(request.getOptions())
                .build();

        AiQuestionValidator.ValidationResult valRes = aiQuestionValidator.validate(draft);
        question.setValidationStatus(valRes.status());
        question.setValidationFeedback(valRes.feedback());

        question = aiGeneratedQuestionRepository.save(question);
        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherAiGeneratedQuestionResponse approveSingleQuestion(UUID jobId, UUID questionId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        AiGeneratedQuestion question = aiGeneratedQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGeneratedQuestion", questionId));

        if (!question.getJob().getId().equals(jobId)) {
            throw new ForbiddenOperationException("Question does not belong to this AI job");
        }

        if (question.getReviewStatus() == AiReviewStatus.APPROVED && question.getApprovedQuestion() != null) {
            return mapToQuestionResponse(question);
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        // Create Real Question in Question Bank
        Question realQ = Question.builder()
                .subject(job.getSubject())
                .category(job.getCategory())
                .course(job.getCourse())
                .lesson(job.getLesson())
                .gradeLevel(job.getGradeLevel())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .content(question.getContent())
                .audioUrl(question.getAudioUrl())
                .audioScript(question.getAudioScript())
                .explanation(question.getExplanation())
                .defaultMarks(question.getMarks())
                .source(QuestionSource.AI_GENERATED)
                .status(QuestionStatus.APPROVED)
                .creator(teacher)
                .build();

        realQ = questionRepository.save(realQ);

        // Create Question Options
        for (AiGeneratedOption opt : question.getOptions()) {
            QuestionOption realOpt = QuestionOption.builder()
                    .question(realQ)
                    .optionKey(opt.getOptionKey())
                    .optionText(opt.getOptionText())
                    .isCorrect(opt.getIsCorrect())
                    .displayOrder(opt.getDisplayOrder())
                    .build();
            questionOptionRepository.save(realOpt);
        }

        question.setReviewStatus(AiReviewStatus.APPROVED);
        question.setApprovedQuestion(realQ);
        question = aiGeneratedQuestionRepository.save(question);

        long approvedCount = aiGeneratedQuestionRepository.countByJobIdAndReviewStatus(jobId, AiReviewStatus.APPROVED);
        job.setTotalApproved((int) approvedCount);
        aiGenerationJobRepository.save(job);

        log.info("Teacher {} approved AI question {} -> Created Question Bank item {}",
                teacherId, questionId, realQ.getId());

        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherAiGeneratedQuestionResponse rejectSingleQuestion(UUID jobId, UUID questionId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        AiGeneratedQuestion question = aiGeneratedQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGeneratedQuestion", questionId));

        if (!question.getJob().getId().equals(jobId)) {
            throw new ForbiddenOperationException("Question does not belong to this AI job");
        }

        question.setReviewStatus(AiReviewStatus.REJECTED);
        question = aiGeneratedQuestionRepository.save(question);

        long approvedCount = aiGeneratedQuestionRepository.countByJobIdAndReviewStatus(jobId, AiReviewStatus.APPROVED);
        job.setTotalApproved((int) approvedCount);
        aiGenerationJobRepository.save(job);

        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherAiApproveJobResponse approveAllValidQuestions(UUID jobId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        List<AiGeneratedQuestion> questions = aiGeneratedQuestionRepository.findByJobIdOrderByDisplayOrderAsc(jobId);
        int approvedThisBatch = 0;

        for (AiGeneratedQuestion q : questions) {
            if (q.getReviewStatus() != AiReviewStatus.APPROVED && q.getValidationStatus() != AiValidationStatus.INVALID) {
                Question realQ = Question.builder()
                        .subject(job.getSubject())
                        .category(job.getCategory())
                        .course(job.getCourse())
                        .lesson(job.getLesson())
                        .gradeLevel(job.getGradeLevel())
                        .questionType(q.getQuestionType())
                        .difficulty(q.getDifficulty())
                        .content(q.getContent())
                        .audioUrl(q.getAudioUrl())
                        .audioScript(q.getAudioScript())
                        .explanation(q.getExplanation())
                        .defaultMarks(q.getMarks())
                        .source(QuestionSource.AI_GENERATED)
                        .status(QuestionStatus.APPROVED)
                        .creator(teacher)
                        .build();

                realQ = questionRepository.save(realQ);

                for (AiGeneratedOption opt : q.getOptions()) {
                    QuestionOption realOpt = QuestionOption.builder()
                            .question(realQ)
                            .optionKey(opt.getOptionKey())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .displayOrder(opt.getDisplayOrder())
                            .build();
                    questionOptionRepository.save(realOpt);
                }

                q.setReviewStatus(AiReviewStatus.APPROVED);
                q.setApprovedQuestion(realQ);
                aiGeneratedQuestionRepository.save(q);
                approvedThisBatch++;
            }
        }

        long totalApproved = aiGeneratedQuestionRepository.countByJobIdAndReviewStatus(jobId, AiReviewStatus.APPROVED);
        job.setTotalApproved((int) totalApproved);
        aiGenerationJobRepository.save(job);

        log.info("Teacher {} bulk-approved {} questions from AI job {}", teacherId, approvedThisBatch, jobId);

        return TeacherAiApproveJobResponse.builder()
                .jobId(jobId)
                .approvedQuestionsCount((int) totalApproved)
                .message(String.format("Đã phê duyệt và thêm %d câu hỏi vào Ngân hàng câu hỏi thành công.", approvedThisBatch))
                .build();
    }

    @Override
    @Transactional
    public TeacherAiApproveJobResponse createExamFromJob(UUID jobId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        if (job.getCreatedExam() != null) {
            return TeacherAiApproveJobResponse.builder()
                    .jobId(jobId)
                    .createdExamId(job.getCreatedExam().getId())
                    .approvedQuestionsCount(job.getTotalApproved())
                    .message("Đề thi đã được tạo từ trước cho phiên AI này.")
                    .build();
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        // 1. Ensure all valid questions are approved
        List<AiGeneratedQuestion> questions = aiGeneratedQuestionRepository.findByJobIdOrderByDisplayOrderAsc(jobId);
        List<Question> approvedRealQuestions = new ArrayList<>();
        BigDecimal totalMarksSum = BigDecimal.ZERO;

        for (AiGeneratedQuestion q : questions) {
            if (q.getValidationStatus() != AiValidationStatus.INVALID && q.getReviewStatus() != AiReviewStatus.REJECTED) {
                if (q.getApprovedQuestion() == null) {
                    Question realQ = Question.builder()
                            .subject(job.getSubject())
                            .course(job.getCourse())
                            .lesson(job.getLesson())
                            .gradeLevel(job.getGradeLevel())
                            .questionType(q.getQuestionType())
                            .difficulty(q.getDifficulty())
                            .content(q.getContent())
                            .audioUrl(q.getAudioUrl())
                            .audioScript(q.getAudioScript())
                            .explanation(q.getExplanation())
                            .defaultMarks(q.getMarks())
                            .source(QuestionSource.AI_GENERATED)
                            .status(QuestionStatus.APPROVED)
                            .creator(teacher)
                            .build();

                    realQ = questionRepository.save(realQ);

                    for (AiGeneratedOption opt : q.getOptions()) {
                        QuestionOption realOpt = QuestionOption.builder()
                                .question(realQ)
                                .optionKey(opt.getOptionKey())
                                .optionText(opt.getOptionText())
                                .isCorrect(opt.getIsCorrect())
                                .displayOrder(opt.getDisplayOrder())
                                .build();
                        questionOptionRepository.save(realOpt);
                    }

                    q.setReviewStatus(AiReviewStatus.APPROVED);
                    q.setApprovedQuestion(realQ);
                    aiGeneratedQuestionRepository.save(q);
                }

                approvedRealQuestions.add(q.getApprovedQuestion());
                totalMarksSum = totalMarksSum.add(q.getMarks() != null ? q.getMarks() : BigDecimal.ONE);
            }
        }

        if (approvedRealQuestions.isEmpty()) {
            throw new ForbiddenOperationException("Không có câu hỏi hợp lệ nào để tạo đề thi");
        }

        // 2. Create Exam
        String examTitle = job.getTargetExamTitle() != null ? job.getTargetExamTitle() : "Đề thi tạo bởi AI";
        String examCode = job.getTargetExamCode() != null ? job.getTargetExamCode() : generateUniqueExamCode(job.getGradeLevel(), job.getSubject().getCode());
        int duration = job.getTargetExamDuration() != null ? job.getTargetExamDuration() : 45;
        BigDecimal passingMarks = job.getTargetExamPassingMarks() != null ? job.getTargetExamPassingMarks() : totalMarksSum.multiply(new BigDecimal("0.5"));

        boolean isListeningExam = approvedRealQuestions.stream().anyMatch(q -> q.getAudioUrl() != null || q.getAudioScript() != null);
        Integer maxPlays = isListeningExam ? 2 : null;

        Exam exam = Exam.builder()
                .subject(job.getSubject())
                .course(job.getCourse())
                .title(examTitle)
                .code(examCode)
                .gradeLevel(job.getGradeLevel())
                .durationMinutes(duration)
                .totalMarks(totalMarksSum)
                .passingMarks(passingMarks)
                .maxListeningPlays(maxPlays)
                .maxAttempts(1)
                .status(ExamStatus.DRAFT)
                .creator(teacher)
                .build();

        exam = examRepository.save(exam);

        // 3. Link Questions via ExamQuestion
        int displayOrder = 1;
        for (Question q : approvedRealQuestions) {
            ExamQuestion eq = ExamQuestion.builder()
                    .exam(exam)
                    .question(q)
                    .displayOrder(displayOrder++)
                    .marks(q.getDefaultMarks() != null ? q.getDefaultMarks() : BigDecimal.ONE)
                    .build();
            examQuestionRepository.save(eq);
        }

        job.setCreatedExam(exam);
        job.setTotalApproved(approvedRealQuestions.size());
        aiGenerationJobRepository.save(job);

        log.info("Teacher {} generated Exam {} ({}) with {} questions from AI Job {}",
                teacherId, exam.getId(), exam.getCode(), approvedRealQuestions.size(), jobId);

        return TeacherAiApproveJobResponse.builder()
                .jobId(jobId)
                .createdExamId(exam.getId())
                .approvedQuestionsCount(approvedRealQuestions.size())
                .message(String.format("Tạo đề thi '%s' (%s) thành công với %d câu hỏi!", exam.getTitle(), exam.getCode(), approvedRealQuestions.size()))
                .build();
    }

    @Override
    @Transactional
    public void deleteJob(UUID jobId, UUID teacherId) {
        AiGenerationJob job = aiGenerationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AiGenerationJob", jobId));
        validateJobTeacher(job, teacherId);

        aiGenerationJobRepository.delete(job);
        log.info("Teacher {} deleted AI generation job {}", teacherId, jobId);
    }

    // --- Helper mappers & validators ---

    private void validateCourseTeacher(Course course, UUID teacherId) {
        if (!SecurityUtils.isAdmin() && !course.getCreator().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("You can only generate questions for your own courses");
        }
    }

    private void validateJobTeacher(AiGenerationJob job, UUID teacherId) {
        if (!SecurityUtils.isAdmin() && !job.getCreator().getId().equals(teacherId)) {
            throw new ForbiddenOperationException("You can only access your own AI generation jobs");
        }
    }

    private String generateUniqueExamCode(String gradeLevel, String subjectCode) {
        String gradePrefix = (gradeLevel != null && !gradeLevel.isBlank())
                ? gradeLevel.replaceAll("\\s+", "").toUpperCase()
                : "GEN";
        String subjPrefix = (subjectCode != null && !subjectCode.isBlank())
                ? subjectCode.toUpperCase()
                : "SUB";

        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = String.format("AI-%s-%s-%s", gradePrefix, subjPrefix, sb);
            if (!examRepository.existsByCode(code)) {
                return code;
            }
        }
        return "AI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TeacherAiJobResponse mapToJobResponse(AiGenerationJob job) {
        return TeacherAiJobResponse.builder()
                .id(job.getId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .promptSummary(job.getPromptSummary())
                .subjectId(job.getSubject() != null ? job.getSubject().getId() : null)
                .subjectName(job.getSubject() != null ? job.getSubject().getName() : null)
                .courseId(job.getCourse() != null ? job.getCourse().getId() : null)
                .courseTitle(job.getCourse() != null ? job.getCourse().getName() : null)
                .lessonId(job.getLesson() != null ? job.getLesson().getId() : null)
                .lessonTitle(job.getLesson() != null ? job.getLesson().getTitle() : null)
                .gradeLevel(job.getGradeLevel())
                .topic(job.getTopic())
                .totalRequested(job.getTotalRequested())
                .totalGenerated(job.getTotalGenerated())
                .totalApproved(job.getTotalApproved())
                .targetExamTitle(job.getTargetExamTitle())
                .targetExamCode(job.getTargetExamCode())
                .targetExamDuration(job.getTargetExamDuration())
                .targetExamPassingMarks(job.getTargetExamPassingMarks())
                .targetExamTotalMarks(job.getTargetExamTotalMarks())
                .createdExamId(job.getCreatedExam() != null ? job.getCreatedExam().getId() : null)
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private void synthesizeAudioForDrafts(List<GeneratedQuestionDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return;
        }

        Map<String, String> scriptToAudioCache = new HashMap<>();

        for (GeneratedQuestionDraft draft : drafts) {
            String script = draft.getAudioScript();
            if (script != null && !script.isBlank() && (draft.getAudioUrl() == null || draft.getAudioUrl().isBlank())) {
                String normalizedScript = script.trim();
                if (scriptToAudioCache.containsKey(normalizedScript)) {
                    draft.setAudioUrl(scriptToAudioCache.get(normalizedScript));
                } else {
                    try {
                        String audioUrl = aiAudioService.synthesizeSpeech(normalizedScript, "Puck");
                        if (audioUrl != null) {
                            draft.setAudioUrl(audioUrl);
                            scriptToAudioCache.put(normalizedScript, audioUrl);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to synthesize audio for listening draft: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private TeacherAiGeneratedQuestionResponse mapToQuestionResponse(AiGeneratedQuestion q) {
        List<TeacherAiGeneratedOptionResponse> opts = q.getOptions().stream()
                .map(opt -> TeacherAiGeneratedOptionResponse.builder()
                        .id(opt.getId())
                        .optionKey(opt.getOptionKey())
                        .optionText(opt.getOptionText())
                        .isCorrect(opt.getIsCorrect())
                        .displayOrder(opt.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return TeacherAiGeneratedQuestionResponse.builder()
                .id(q.getId())
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .difficulty(q.getDifficulty())
                .marks(q.getMarks())
                .audioUrl(q.getAudioUrl())
                .audioScript(q.getAudioScript())
                .explanation(q.getExplanation())
                .tags(q.getTags())
                .displayOrder(q.getDisplayOrder())
                .validationStatus(q.getValidationStatus())
                .validationFeedback(q.getValidationFeedback())
                .reviewStatus(q.getReviewStatus())
                .approvedQuestionId(q.getApprovedQuestion() != null ? q.getApprovedQuestion().getId() : null)
                .options(opts)
                .build();
    }

    private TeacherAiJobDetailResponse mapToJobDetailResponse(AiGenerationJob job, List<AiGeneratedQuestion> questions) {
        return TeacherAiJobDetailResponse.builder()
                .job(mapToJobResponse(job))
                .questions(questions.stream().map(this::mapToQuestionResponse).collect(Collectors.toList()))
                .build();
    }
}
