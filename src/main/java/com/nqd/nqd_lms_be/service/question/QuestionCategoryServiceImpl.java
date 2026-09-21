package com.nqd.nqd_lms_be.service.question;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryRequest;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryResponse;
import com.nqd.nqd_lms_be.entity.QuestionCategory;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
import com.nqd.nqd_lms_be.repository.QuestionCategoryRepository;
import com.nqd.nqd_lms_be.repository.SubjectRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionCategoryServiceImpl implements QuestionCategoryService {

    private final QuestionCategoryRepository questionCategoryRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<QuestionCategoryResponse> getCategories(UUID subjectId, String gradeLevel, UUID userId) {
        List<QuestionCategory> categories;
        if (subjectId != null && gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equalsIgnoreCase("ALL")) {
            String cleanGrade = gradeLevel.trim();
            ensureDefaultPublicCategoryExists(subjectId, cleanGrade);
            categories = questionCategoryRepository
                    .findBySubjectIdAndGradeLevelAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(subjectId, cleanGrade);
        } else if (subjectId != null) {
            categories = questionCategoryRepository
                    .findBySubjectIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(subjectId);
        } else {
            categories = questionCategoryRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .sorted(Comparator.comparing(QuestionCategory::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
        }

        // Visibility Filtering:
        // 1. PUBLIC: tất cả mọi người trong hệ thống đều xem được
        // 2. TEACHER_SHARED: cho người tạo, admin và giáo viên xem được
        // 3. PRIVATE: chỉ mỗi người tạo và admin xem được
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isTeacher = SecurityUtils.isTeacher();

        List<QuestionCategory> visibleCategories = categories.stream()
                .filter(c -> {
                    if (isAdmin) return true;
                    QuestionCategoryVisibility vis = c.getVisibility() != null
                            ? c.getVisibility()
                            : QuestionCategoryVisibility.TEACHER_SHARED;

                    if (vis == QuestionCategoryVisibility.PUBLIC) {
                        return true;
                    }
                    if (isTeacher) {
                        if (vis == QuestionCategoryVisibility.TEACHER_SHARED) {
                            return true;
                        }
                        if (vis == QuestionCategoryVisibility.PRIVATE) {
                            return c.getCreator() != null && c.getCreator().getId().equals(userId);
                        }
                    }
                    // For student / others, only PUBLIC is visible
                    return false;
                })
                .collect(Collectors.toList());

        // Map question counts
        Map<UUID, Long> countMap = new HashMap<>();
        List<Object[]> rawCounts = questionCategoryRepository.countQuestionsByCategory();
        for (Object[] row : rawCounts) {
            if (row != null && row.length >= 2 && row[0] instanceof UUID) {
                countMap.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        return visibleCategories.stream()
                .map(c -> mapToResponse(c, countMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionCategoryResponse getCategoryById(UUID id, UUID userId) {
        QuestionCategory category = questionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", id));

        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isTeacher = SecurityUtils.isTeacher();
        QuestionCategoryVisibility vis = category.getVisibility() != null ? category.getVisibility() : QuestionCategoryVisibility.TEACHER_SHARED;

        if (!isAdmin) {
            if (vis == QuestionCategoryVisibility.PRIVATE && (category.getCreator() == null || !category.getCreator().getId().equals(userId))) {
                throw new ForbiddenOperationException("Bạn không có quyền xem chuyên đề riêng tư này.");
            }
            if (vis == QuestionCategoryVisibility.TEACHER_SHARED && !isTeacher) {
                throw new ForbiddenOperationException("Chuyên đề này chỉ dành cho giáo viên và ban quản trị.");
            }
        }

        long count = questionCategoryRepository.countQuestionsByCategoryId(category.getId());
        return mapToResponse(category, count);
    }

    @Override
    @Transactional
    public QuestionCategoryResponse createCategory(QuestionCategoryRequest request, UUID userId) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        QuestionCategory parent = null;
        if (request.getParentId() != null) {
            parent = questionCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", request.getParentId()));
        }

        QuestionCategoryVisibility visibility = request.getVisibility() != null
                ? request.getVisibility()
                : QuestionCategoryVisibility.TEACHER_SHARED;

        QuestionCategory category = QuestionCategory.builder()
                .name(request.getName().trim())
                .code(request.getCode() != null ? request.getCode().trim() : null)
                .description(request.getDescription())
                .subject(subject)
                .gradeLevel(request.getGradeLevel() != null ? request.getGradeLevel().trim() : null)
                .parent(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .visibility(visibility)
                .creator(creator)
                .isSystem(Boolean.TRUE.equals(request.getIsSystem()) && SecurityUtils.isAdmin())
                .build();

        category = questionCategoryRepository.save(category);
        log.info("User {} created QuestionCategory: {} (id={}, visibility={})", userId, category.getName(), category.getId(), visibility);
        return mapToResponse(category, 0L);
    }

    @Override
    @Transactional
    public QuestionCategoryResponse updateCategory(UUID id, QuestionCategoryRequest request, UUID userId) {
        QuestionCategory category = questionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", id));

        if (!SecurityUtils.isAdmin() && category.getCreator() != null && !category.getCreator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn chỉ có quyền sửa danh mục do chính bạn tạo.");
        }

        if (request.getSubjectId() != null && !request.getSubjectId().equals(category.getSubject().getId())) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));
            category.setSubject(subject);
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Danh mục cha không thể là chính nó.");
            }
            QuestionCategory parent = questionCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category.setName(request.getName().trim());
        if (request.getCode() != null) {
            category.setCode(request.getCode().trim());
        }
        category.setDescription(request.getDescription());
        if (request.getGradeLevel() != null) {
            category.setGradeLevel(request.getGradeLevel().trim());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getVisibility() != null) {
            category.setVisibility(request.getVisibility());
        }

        category = questionCategoryRepository.save(category);
        long count = questionCategoryRepository.countQuestionsByCategoryId(category.getId());
        log.info("User {} updated QuestionCategory: {} (visibility={})", userId, category.getId(), category.getVisibility());
        return mapToResponse(category, count);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id, UUID userId) {
        QuestionCategory category = questionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionCategory", id));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw new ForbiddenOperationException("Không thể xóa chuyên đề mặc định của hệ thống.");
        }

        if (!SecurityUtils.isAdmin() && category.getCreator() != null && !category.getCreator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn chỉ có quyền xóa danh mục do chính bạn tạo.");
        }

        category.setIsDeleted(true);
        category.setDeletedAt(LocalDateTime.now());
        category.setDeletedBy(userId.toString());
        questionCategoryRepository.save(category);
        log.info("User {} soft-deleted QuestionCategory: {}", userId, id);
    }

    private void ensureDefaultPublicCategoryExists(UUID subjectId, String gradeLevel) {
        boolean hasPublic = questionCategoryRepository
                .findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
                        subjectId, gradeLevel, QuestionCategoryVisibility.PUBLIC
                ).isPresent();

        if (!hasPublic) {
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject != null) {
                String cleanGradeCode = gradeLevel.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                QuestionCategory cat = QuestionCategory.builder()
                        .name("Chuyên đề chung")
                        .code("GENERAL_" + subject.getCode() + "_" + cleanGradeCode)
                        .description("Chuyên đề chung mặc định công khai cho " + subject.getName() + " - " + gradeLevel)
                        .subject(subject)
                        .gradeLevel(gradeLevel)
                        .visibility(QuestionCategoryVisibility.PUBLIC)
                        .isSystem(true)
                        .displayOrder(0)
                        .build();
                questionCategoryRepository.save(cat);
                log.info("Auto-provisioned default PUBLIC category for Subject {} - Grade {}", subject.getName(), gradeLevel);
            }
        }
    }

    private QuestionCategoryResponse mapToResponse(QuestionCategory category, long count) {
        return QuestionCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .description(category.getDescription())
                .subjectId(category.getSubject() != null ? category.getSubject().getId() : null)
                .subjectName(category.getSubject() != null ? category.getSubject().getName() : null)
                .gradeLevel(category.getGradeLevel())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .displayOrder(category.getDisplayOrder())
                .creatorId(category.getCreator() != null ? category.getCreator().getId() : null)
                .creatorName(category.getCreator() != null ? category.getCreator().getFullName() : null)
                .visibility(category.getVisibility() != null ? category.getVisibility() : QuestionCategoryVisibility.TEACHER_SHARED)
                .isSystem(category.getIsSystem())
                .questionCount(count)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
