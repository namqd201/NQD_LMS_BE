package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.report.QuestionPaperExportRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionResponse;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.service.report.ExamPaperExportService;
import com.nqd.nqd_lms_be.service.teacher.TeacherQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Question Bank", description = "Endpoints for question bank management")
public class TeacherQuestionBankController {

    private final TeacherQuestionService teacherQuestionService;
    private final ExamPaperExportService examPaperExportService;

    @GetMapping
    @Operation(summary = "Get question bank list with full details, explanations and answers")
    public ResponseEntity<List<TeacherQuestionResponse>> getQuestions(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.getQuestions(
                subjectId, categoryId, courseId, lessonId, gradeLevel, questionType, difficulty, status, tag, keyword, principal.getId()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get question details by ID")
    public ResponseEntity<TeacherQuestionResponse> getQuestionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.getQuestionById(id, principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a question in the question bank (Teacher/Admin only)")
    public ResponseEntity<TeacherQuestionResponse> createQuestion(
            @Valid @RequestBody TeacherQuestionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherQuestionResponse response = teacherQuestionService.createQuestion(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a question (Strict author ownership check enforced)")
    public ResponseEntity<TeacherQuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherQuestionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.updateQuestion(id, request, principal.getId()));
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Archive a question (Strict author ownership check enforced)")
    public ResponseEntity<TeacherQuestionResponse> archiveQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.archiveQuestion(id, principal.getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update question status (Strict author ownership check enforced)")
    public ResponseEntity<TeacherQuestionResponse> updateQuestionStatus(
            @PathVariable UUID id,
            @RequestParam QuestionStatus status,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.updateQuestionStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a question (Soft delete with author ownership check)")
    public ResponseEntity<MessageResponse> deleteQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherQuestionService.deleteQuestion(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã chuyển câu hỏi vào thùng rác"));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get deleted questions history / trash")
    public ResponseEntity<List<TeacherQuestionResponse>> getDeletedQuestions(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.getDeletedQuestions(principal.getId()));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Restore a soft-deleted question")
    public ResponseEntity<TeacherQuestionResponse> restoreQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherQuestionService.restoreQuestion(id, principal.getId()));
    }

    @PostMapping("/export/docx")
    @Operation(summary = "Export question bank list to Word (.docx) without answers for students and teachers")
    public ResponseEntity<byte[]> exportQuestionsDocx(
            @RequestBody(required = false) QuestionPaperExportRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        if (request == null) {
            request = new QuestionPaperExportRequest();
        }
        byte[] docxBytes = examPaperExportService.exportQuestionsDocx(request, principal.getId());

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "De_On_Tap_" + dateStr + ".docx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxBytes);
    }

    @PostMapping("/export/pdf")
    @Operation(summary = "Export question bank list to PDF (.pdf) without answers for students and teachers")
    public ResponseEntity<byte[]> exportQuestionsPdf(
            @RequestBody(required = false) QuestionPaperExportRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        if (request == null) {
            request = new QuestionPaperExportRequest();
        }
        byte[] pdfBytes = examPaperExportService.exportQuestionsPdf(request, principal.getId());

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "De_On_Tap_" + dateStr + ".pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
