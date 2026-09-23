package com.nqd.nqd_lms_be.controller.knowledge;

import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeCurriculumResponse;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeLessonResponse;
import com.nqd.nqd_lms_be.service.knowledge.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/knowledge")
@RequiredArgsConstructor
@Tag(name = "Public - Knowledge Base", description = "Endpoints for students & visitors to access GDPT standard curriculum and theory/quizzes")
public class PublicKnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/curriculums")
    @Operation(summary = "Get curriculum by subject and grade level")
    public ResponseEntity<KnowledgeCurriculumResponse> getCurriculumBySubjectAndGrade(
            @RequestParam String subject,
            @RequestParam String gradeLevel
    ) {
        return ResponseEntity.ok(knowledgeService.getCurriculumBySubjectAndGrade(subject, gradeLevel));
    }

    @GetMapping("/curriculums/{code}")
    @Operation(summary = "Get curriculum by unique code (e.g. MATH_GRADE_1)")
    public ResponseEntity<KnowledgeCurriculumResponse> getCurriculumByCode(@PathVariable String code) {
        return ResponseEntity.ok(knowledgeService.getCurriculumByCode(code));
    }

    @GetMapping("/subjects/{subjectId}/curriculums")
    @Operation(summary = "Get all published curriculums for a subject")
    public ResponseEntity<List<KnowledgeCurriculumResponse>> getCurriculumsBySubject(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(knowledgeService.getCurriculumsBySubject(subjectId));
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Get detailed theory and reinforcement questions for a lesson")
    public ResponseEntity<KnowledgeLessonResponse> getLessonDetail(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(knowledgeService.getLessonDetail(lessonId));
    }
}
