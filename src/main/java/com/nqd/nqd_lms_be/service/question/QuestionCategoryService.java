package com.nqd.nqd_lms_be.service.question;

import com.nqd.nqd_lms_be.dto.question.QuestionCategoryRequest;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface QuestionCategoryService {

    List<QuestionCategoryResponse> getCategories(UUID subjectId, String gradeLevel, UUID userId);

    QuestionCategoryResponse getCategoryById(UUID id, UUID userId);

    QuestionCategoryResponse createCategory(QuestionCategoryRequest request, UUID userId);

    QuestionCategoryResponse updateCategory(UUID id, QuestionCategoryRequest request, UUID userId);

    void deleteCategory(UUID id, UUID userId);
}
