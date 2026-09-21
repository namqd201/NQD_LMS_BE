package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionOptionDto;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class TeacherQuestionServiceImpl implements TeacherQuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final QuestionTagRepository questionTagRepository;
    private final QuestionTagRelationRepository questionTagRelationRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherQuestionResponse> getQuestions(
            UUID subjectId,
            UUID categoryId,
            UUID courseId,
            UUID lessonId,
            String gradeLevel,
            QuestionType questionType,
            QuestionDifficulty difficulty,
            QuestionStatus status,
            String tag,
            String keyword,
            UUID userId
    ) {
        Specification<Question> spec = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));

        // 1. Role & Visibility filtering:
        // - Students only see approved questions (APPROVED)
        // - Teachers and Admins can see all APPROVED questions PLUS their own drafts/reviews
        if (SecurityUtils.isStudent()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), QuestionStatus.APPROVED));
        } else if (status != null) {
            // If explicit status requested, apply it
            if (status == QuestionStatus.DRAFT) {
                // Drafts only visible to their creator
                spec = spec.and((root, query, cb) -> cb.and(
                        cb.equal(root.get("status"), QuestionStatus.DRAFT),
                        cb.equal(root.get("creator").get("id"), userId)
                ));
            } else {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            }
        } else {
            // Default for Teacher/Admin: show all APPROVED questions OR questions created by themselves
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("status"), QuestionStatus.APPROVED),
                    cb.equal(root.get("creator").get("id"), userId)
            ));
        }

        // 2. Subject filter
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }

        // 2.1 Category filter
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        // 3. Course filter
        if (courseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("course").get("id"), courseId));
        }

        // 4. Lesson filter
        if (lessonId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lesson").get("id"), lessonId));
        }

        // 5. Grade level filter
        if (gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equalsIgnoreCase("ALL")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gradeLevel"), gradeLevel.trim()));
        }

        // 6. Question Type filter
        if (questionType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("questionType"), questionType));
        }

        // 7. Difficulty filter
        if (difficulty != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        }

        // 8. Status filter
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        // 9. Keyword search (content or explanation)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("content")), pattern),
                    cb.like(cb.lower(root.get("explanation")), pattern)
            ));
        }

        List<Question> questions = questionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 10. Tag filter if provided
        if (tag != null && !tag.trim().isEmpty()) {
            String targetTag = tag.trim().toLowerCase();
            return questions.stream()
                    .map(this::mapToQuestionResponse)
                    .filter(q -> q.getTags() != null && q.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(targetTag)))
                    .collect(Collectors.toList());
        }

        return questions.stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherQuestionResponse getQuestionById(UUID questionId, UUID teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherQuestionResponse createQuestion(TeacherQuestionRequest request, UUID teacherId) {
        // Enforce Question Bank limit for Free Teacher
        membershipEntitlementService.enforceAndConsumeUsage(
                teacherId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.QUESTION_BANK_LIMIT, 1
        );

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        User creator = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
        }

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()));
        }

        String effectiveGradeLevel = request.getGradeLevel();
        if ((effectiveGradeLevel == null || effectiveGradeLevel.trim().isEmpty()) && course != null) {
            effectiveGradeLevel = course.getGradeLevel();
        }

        QuestionCategory category = null;
        if (request.getCategoryId() != null) {
            category = questionCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", request.getCategoryId()));
        } else if (subject != null && effectiveGradeLevel != null && !effectiveGradeLevel.trim().isEmpty()) {
            category = questionCategoryRepository
                    .findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
                            subject.getId(), effectiveGradeLevel.trim(), QuestionCategoryVisibility.PUBLIC
                    ).orElse(null);
        }

        Question question = Question.builder()
                .subject(subject)
                .category(category)
                .course(course)
                .lesson(lesson)
                .gradeLevel(effectiveGradeLevel)
                .questionType(request.getQuestionType())
                .difficulty(request.getDifficulty())
                .content(request.getContent())
                .explanation(request.getExplanation())
                .defaultMarks(request.getDefaultMarks() != null ? request.getDefaultMarks() : new BigDecimal("1.00"))
                .status(request.getStatus() != null ? request.getStatus() : QuestionStatus.DRAFT)
                .creator(creator)
                .build();

        question = questionRepository.save(question);

        // Save options
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            int order = 1;
            for (TeacherQuestionOptionDto optDto : request.getOptions()) {
                QuestionOption option = QuestionOption.builder()
                        .question(question)
                        .optionKey(optDto.getOptionKey() != null ? optDto.getOptionKey() : String.valueOf((char) ('A' + order - 1)))
                        .optionText(optDto.getOptionText())
                        .isCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()))
                        .displayOrder(optDto.getDisplayOrder() != null && optDto.getDisplayOrder() > 0 ? optDto.getDisplayOrder() : order)
                        .build();
                questionOptionRepository.save(option);
                order++;
            }
        }

        // Save tags
        syncQuestionTags(question, request.getTags());

        log.info("Teacher {} created question: {}", creator.getEmail(), question.getId());
        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherQuestionResponse updateQuestion(UUID questionId, TeacherQuestionRequest request, UUID teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        verifyQuestionOwnership(question, teacherId);

        if (request.getSubjectId() != null && !request.getSubjectId().equals(question.getSubject().getId())) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));
            question.setSubject(subject);
        }

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
            question.setCourse(course);
        } else {
            question.setCourse(null);
        }

        if (request.getCategoryId() != null) {
            QuestionCategory category = questionCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", request.getCategoryId()));
            question.setCategory(category);
        } else {
            question.setCategory(null);
        }

        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()));
            question.setLesson(lesson);
        } else {
            question.setLesson(null);
        }

        if (request.getGradeLevel() != null) {
            question.setGradeLevel(request.getGradeLevel());
        }

        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setContent(request.getContent());
        question.setExplanation(request.getExplanation());
        if (request.getDefaultMarks() != null) {
            question.setDefaultMarks(request.getDefaultMarks());
        }
        if (request.getStatus() != null) {
            question.setStatus(request.getStatus());
        }

        question = questionRepository.saveAndFlush(question);

        if (request.getOptions() != null) {
            questionOptionRepository.deleteByQuestionId(question.getId());
            if (!request.getOptions().isEmpty()) {
                List<QuestionOption> newOptions = new ArrayList<>();
                int order = 1;
                for (TeacherQuestionOptionDto optDto : request.getOptions()) {
                    QuestionOption option = QuestionOption.builder()
                            .question(question)
                            .optionKey(optDto.getOptionKey() != null ? optDto.getOptionKey() : String.valueOf((char) ('A' + order - 1)))
                            .optionText(optDto.getOptionText())
                            .isCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()))
                            .displayOrder(optDto.getDisplayOrder() != null && optDto.getDisplayOrder() > 0 ? optDto.getDisplayOrder() : order)
                            .build();
                    newOptions.add(option);
                    order++;
                }
                questionOptionRepository.saveAllAndFlush(newOptions);
            }
        }

        // Sync tags
        if (request.getTags() != null) {
            questionTagRelationRepository.deleteByQuestionId(question.getId());
            syncQuestionTags(question, request.getTags());
        }

        log.info("Teacher {} updated question: {}", teacherId, question.getId());
        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherQuestionResponse updateQuestionStatus(UUID questionId, QuestionStatus status, UUID teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        if (!SecurityUtils.isAdmin()) {
            verifyQuestionOwnership(question, teacherId);
        }
        question.setStatus(status);
        question = questionRepository.save(question);
        log.info("User {} updated question {} status to: {}", teacherId, questionId, status);
        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public TeacherQuestionResponse archiveQuestion(UUID questionId, UUID teacherId) {
        return updateQuestionStatus(questionId, QuestionStatus.ARCHIVED, teacherId);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId, UUID teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        verifyQuestionOwnership(question, teacherId);

        User user = userRepository.findById(teacherId).orElse(null);
        String userEmail = user != null ? user.getEmail() : teacherId.toString();

        question.setIsDeleted(true);
        question.setDeletedAt(LocalDateTime.now());
        question.setDeletedBy(userEmail);
        questionRepository.save(question);

        log.info("Teacher/User {} soft-deleted question: {}", userEmail, question.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherQuestionResponse> getDeletedQuestions(UUID teacherId) {
        Specification<Question> spec = (root, query, cb) -> cb.isTrue(root.get("isDeleted"));

        if (!SecurityUtils.isAdmin()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), teacherId));
        }

        List<Question> deletedQuestions = questionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "deletedAt"));
        return deletedQuestions.stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherQuestionResponse restoreQuestion(UUID questionId, UUID teacherId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        verifyQuestionOwnership(question, teacherId);

        question.setIsDeleted(false);
        question.setDeletedAt(null);
        question.setDeletedBy(null);
        question = questionRepository.save(question);

        log.info("Teacher/User {} restored question: {}", teacherId, question.getId());
        return mapToQuestionResponse(question);
    }

    private void syncQuestionTags(Question question, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        for (String rawName : tagNames) {
            if (rawName == null || rawName.trim().isEmpty()) {
                continue;
            }
            String normalizedName = rawName.trim();
            QuestionTag tag = questionTagRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> questionTagRepository.save(QuestionTag.builder().name(normalizedName).build()));

            QuestionTagRelation relation = QuestionTagRelation.builder()
                    .questionId(question.getId())
                    .tagId(tag.getId())
                    .question(question)
                    .tag(tag)
                    .build();

            questionTagRelationRepository.save(relation);
        }
    }

    private void verifyQuestionOwnership(Question question, UUID userId) {
        if (question.getCreator() == null || !question.getCreator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn chỉ có quyền chỉnh sửa hoặc xóa câu hỏi do chính mình tạo ra.");
        }
    }

    private TeacherQuestionResponse mapToQuestionResponse(Question question) {
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(question.getId());
        List<TeacherQuestionOptionDto> optionDtos = options != null ? options.stream()
                .map(opt -> TeacherQuestionOptionDto.builder()
                        .id(opt.getId())
                        .optionKey(opt.getOptionKey())
                        .optionText(opt.getOptionText())
                        .isCorrect(opt.getIsCorrect())
                        .displayOrder(opt.getDisplayOrder())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        List<String> tags = questionTagRelationRepository.findTagNamesByQuestionId(question.getId());

        return TeacherQuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubject() != null ? question.getSubject().getId() : null)
                .subjectName(question.getSubject() != null ? question.getSubject().getName() : null)
                .categoryId(question.getCategory() != null ? question.getCategory().getId() : null)
                .categoryName(question.getCategory() != null ? question.getCategory().getName() : null)
                .courseId(question.getCourse() != null ? question.getCourse().getId() : null)
                .courseName(question.getCourse() != null ? question.getCourse().getName() : null)
                .lessonId(question.getLesson() != null ? question.getLesson().getId() : null)
                .lessonTitle(question.getLesson() != null ? question.getLesson().getTitle() : null)
                .gradeLevel(question.getGradeLevel())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .content(question.getContent())
                .explanation(question.getExplanation())
                .defaultMarks(question.getDefaultMarks())
                .status(question.getStatus())
                .creatorId(question.getCreator() != null ? question.getCreator().getId() : null)
                .creatorName(question.getCreator() != null ? question.getCreator().getFullName() : null)
                .tags(tags)
                .options(optionDtos)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .isDeleted(question.getIsDeleted())
                .deletedAt(question.getDeletedAt())
                .deletedBy(question.getDeletedBy())
                .build();
    }
}
