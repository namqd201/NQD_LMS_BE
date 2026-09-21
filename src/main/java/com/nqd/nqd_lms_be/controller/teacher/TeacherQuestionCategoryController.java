package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryRequest;
import com.nqd.nqd_lms_be.dto.question.QuestionCategoryResponse;
import com.nqd.nqd_lms_be.service.question.QuestionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/question-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Question Categories", description = "Endpoints for managing question categories and topics")
public class TeacherQuestionCategoryController {

    private final QuestionCategoryService questionCategoryService;

    @GetMapping
    @Operation(summary = "Get list of question categories by subject and grade level")
    public ResponseEntity<List<QuestionCategoryResponse>> getCategories(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String gradeLevel,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(questionCategoryService.getCategories(subjectId, gradeLevel, principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get question category details by ID")
    public ResponseEntity<QuestionCategoryResponse> getCategoryById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(questionCategoryService.getCategoryById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new question category / topic")
    public ResponseEntity<QuestionCategoryResponse> createCategory(
            @Valid @RequestBody QuestionCategoryRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        QuestionCategoryResponse response = questionCategoryService.createCategory(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing question category")
    public ResponseEntity<QuestionCategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionCategoryRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(questionCategoryService.updateCategory(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (soft-delete) a question category")
    public ResponseEntity<MessageResponse> deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        questionCategoryService.deleteCategory(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa chuyên đề câu hỏi thành công"));
    }
}
