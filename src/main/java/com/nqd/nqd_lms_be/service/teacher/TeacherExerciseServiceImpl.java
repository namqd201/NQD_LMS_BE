package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.exercise.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherExerciseServiceImpl implements TeacherExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherExerciseResponse> getExercises(
            UUID lessonId,
            UUID courseId,
            UUID subjectId,
            ExerciseStatus status,
            String keyword,
            UUID teacherId
    ) {
        Specification<Exercise> spec = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        if (!SecurityUtils.isAdmin()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), teacherId));
        }

        if (lessonId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lesson").get("id"), lessonId));
        }

        if (courseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lesson").get("chapter").get("course").get("id"), courseId));
        }

        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lesson").get("chapter").get("course").get("subject").get("id"), subjectId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern));
        }

        List<Exercise> exercises = exerciseRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return exercises.stream().map(this::mapToResponseSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherExerciseResponse getExerciseById(UUID id, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        return mapToResponseDetail(exercise);
    }

    @Override
    @Transactional
    public TeacherExerciseResponse createExercise(TeacherExerciseRequest request, UUID teacherId) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + request.getLessonId()));

        User creator = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với ID: " + teacherId));

        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .type(request.getType() != null ? request.getType() : com.nqd.nqd_lms_be.entity.enums.ExerciseType.PRACTICE)
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore())
                .status(request.getStatus() != null ? request.getStatus() : ExerciseStatus.DRAFT)
                .maxAttempts(request.getMaxAttempts())
                .showExplanationImmediately(request.getShowExplanationImmediately() != null ? request.getShowExplanationImmediately() : true)
                .allowRetry(request.getAllowRetry() != null ? request.getAllowRetry() : true)
                .creator(creator)
                .build();

        Exercise saved = exerciseRepository.save(exercise);

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int order = 1;
            for (TeacherExerciseQuestionItemRequest qReq : request.getQuestions()) {
                Question question = questionRepository.findById(qReq.getQuestionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + qReq.getQuestionId()));

                BigDecimal marks = qReq.getMarks() != null ? qReq.getMarks() : question.getDefaultMarks();
                int displayOrder = qReq.getDisplayOrder() != null ? qReq.getDisplayOrder() : order++;

                ExerciseQuestion eq = ExerciseQuestion.builder()
                        .exerciseId(saved.getId())
                        .questionId(question.getId())
                        .exercise(saved)
                        .question(question)
                        .displayOrder(displayOrder)
                        .marks(marks)
                        .build();

                exerciseQuestionRepository.save(eq);
            }
        }

        return mapToResponseDetail(saved);
    }

    @Override
    @Transactional
    public TeacherExerciseResponse updateExercise(UUID id, TeacherExerciseRequest request, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);

        if (request.getLessonId() != null && !request.getLessonId().equals(exercise.getLesson().getId())) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
            exercise.setLesson(lesson);
        }

        exercise.setTitle(request.getTitle());
        exercise.setDescription(request.getDescription());
        exercise.setInstructions(request.getInstructions());
        if (request.getType() != null) exercise.setType(request.getType());
        exercise.setTimeLimitMinutes(request.getTimeLimitMinutes());
        exercise.setPassingScore(request.getPassingScore());
        if (request.getStatus() != null) exercise.setStatus(request.getStatus());
        exercise.setMaxAttempts(request.getMaxAttempts());
        if (request.getShowExplanationImmediately() != null) exercise.setShowExplanationImmediately(request.getShowExplanationImmediately());
        if (request.getAllowRetry() != null) exercise.setAllowRetry(request.getAllowRetry());

        Exercise updated = exerciseRepository.save(exercise);

        if (request.getQuestions() != null) {
            exerciseQuestionRepository.deleteByExerciseId(updated.getId());
            int order = 1;
            for (TeacherExerciseQuestionItemRequest qReq : request.getQuestions()) {
                Question question = questionRepository.findById(qReq.getQuestionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi: " + qReq.getQuestionId()));

                BigDecimal marks = qReq.getMarks() != null ? qReq.getMarks() : question.getDefaultMarks();
                int displayOrder = qReq.getDisplayOrder() != null ? qReq.getDisplayOrder() : order++;

                ExerciseQuestion eq = ExerciseQuestion.builder()
                        .exerciseId(updated.getId())
                        .questionId(question.getId())
                        .exercise(updated)
                        .question(question)
                        .displayOrder(displayOrder)
                        .marks(marks)
                        .build();

                exerciseQuestionRepository.save(eq);
            }
        }

        return mapToResponseDetail(updated);
    }

    @Override
    @Transactional
    public TeacherExerciseResponse publishExercise(UUID id, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        long qCount = exerciseQuestionRepository.countByExerciseId(id);
        if (qCount == 0) {
            throw new IllegalArgumentException("Không thể phát hành bài tập chưa có câu hỏi nào.");
        }
        exercise.setStatus(ExerciseStatus.PUBLISHED);
        return mapToResponseDetail(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public TeacherExerciseResponse archiveExercise(UUID id, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        exercise.setStatus(ExerciseStatus.ARCHIVED);
        return mapToResponseDetail(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public void deleteExercise(UUID id, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        exercise.setIsDeleted(true);
        exerciseRepository.save(exercise);
    }

    @Override
    @Transactional
    public TeacherExerciseResponse addQuestionToExercise(UUID id, TeacherExerciseQuestionItemRequest itemRequest, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        Question question = questionRepository.findById(itemRequest.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + itemRequest.getQuestionId()));

        BigDecimal marks = itemRequest.getMarks() != null ? itemRequest.getMarks() :
                (question.getDefaultMarks() != null ? question.getDefaultMarks() : BigDecimal.ONE);

        Optional<ExerciseQuestion> existing = exerciseQuestionRepository.findByExerciseIdAndQuestionId(id, question.getId());
        if (existing.isPresent()) {
            ExerciseQuestion eq = existing.get();
            eq.setMarks(marks);
            if (itemRequest.getDisplayOrder() != null) {
                eq.setDisplayOrder(itemRequest.getDisplayOrder());
            }
            exerciseQuestionRepository.save(eq);
            return mapToResponseDetail(exercise);
        }

        int maxOrder = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(id)
                .stream().mapToInt(ExerciseQuestion::getDisplayOrder).max().orElse(0);

        int displayOrder = itemRequest.getDisplayOrder() != null ? itemRequest.getDisplayOrder() : (maxOrder + 1);

        ExerciseQuestion eq = ExerciseQuestion.builder()
                .exerciseId(exercise.getId())
                .questionId(question.getId())
                .exercise(exercise)
                .question(question)
                .displayOrder(displayOrder)
                .marks(marks)
                .build();

        exerciseQuestionRepository.save(eq);
        return mapToResponseDetail(exercise);
    }

    @Override
    @Transactional
    public TeacherExerciseResponse removeQuestionFromExercise(UUID id, UUID questionId, UUID teacherId) {
        Exercise exercise = findAndValidateOwnership(id, teacherId);
        exerciseQuestionRepository.deleteByExerciseIdAndQuestionId(id, questionId);
        return mapToResponseDetail(exercise);
    }

    private Exercise findAndValidateOwnership(UUID id, UUID teacherId) {
        Exercise exercise = exerciseRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + id));

        if (!SecurityUtils.isAdmin()) {
            if (exercise.getCreator() != null && !exercise.getCreator().getId().equals(teacherId)) {
                throw new ForbiddenOperationException("Bạn không có quyền thao tác trên bài tập này.");
            }
        }
        return exercise;
    }

    private TeacherExerciseResponse mapToResponseSummary(Exercise e) {
        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(e.getId());
        BigDecimal totalMarks = eqList.stream()
                .map(ExerciseQuestion::getMarks)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Lesson lesson = e.getLesson();
        Chapter chapter = lesson != null ? lesson.getChapter() : null;
        Course course = chapter != null ? chapter.getCourse() : null;
        Subject subject = course != null ? course.getSubject() : null;

        return TeacherExerciseResponse.builder()
                .id(e.getId())
                .lessonId(lesson != null ? lesson.getId() : null)
                .lessonTitle(lesson != null ? lesson.getTitle() : null)
                .chapterId(chapter != null ? chapter.getId() : null)
                .chapterTitle(chapter != null ? chapter.getTitle() : null)
                .courseId(course != null ? course.getId() : null)
                .courseTitle(course != null ? course.getName() : null)
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .title(e.getTitle())
                .description(e.getDescription())
                .instructions(e.getInstructions())
                .type(e.getType())
                .timeLimitMinutes(e.getTimeLimitMinutes())
                .passingScore(e.getPassingScore())
                .status(e.getStatus())
                .maxAttempts(e.getMaxAttempts())
                .showExplanationImmediately(e.getShowExplanationImmediately())
                .allowRetry(e.getAllowRetry())
                .questionCount(eqList.size())
                .totalMarks(totalMarks)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .creatorId(e.getCreator() != null ? e.getCreator().getId() : null)
                .creatorName(e.getCreator() != null ? e.getCreator().getFullName() : null)
                .build();
    }

    private TeacherExerciseResponse mapToResponseDetail(Exercise e) {
        TeacherExerciseResponse response = mapToResponseSummary(e);
        List<ExerciseQuestion> eqList = exerciseQuestionRepository.findByExerciseIdOrderByDisplayOrderAsc(e.getId());

        List<TeacherExerciseQuestionResponse> questions = eqList.stream().map(eq -> {
            Question q = eq.getQuestion();
            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());

            List<TeacherExerciseOptionResponse> optResponses = options.stream().map(opt ->
                    TeacherExerciseOptionResponse.builder()
                            .id(opt.getId())
                            .optionKey(opt.getOptionKey())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .displayOrder(opt.getDisplayOrder())
                            .build()
            ).collect(Collectors.toList());

            return TeacherExerciseQuestionResponse.builder()
                    .questionId(q.getId())
                    .displayOrder(eq.getDisplayOrder())
                    .marks(eq.getMarks())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .explanation(q.getExplanation())
                    .options(optResponses)
                    .build();
        }).collect(Collectors.toList());

        response.setQuestions(questions);
        return response;
    }
}
