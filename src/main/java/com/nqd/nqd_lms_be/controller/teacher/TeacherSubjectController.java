package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.dto.admin.SubjectResponse;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teacher/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Subjects", description = "Endpoints to query academic subjects")
public class TeacherSubjectController {

    private final SubjectRepository subjectRepository;

    @GetMapping
    @Operation(summary = "Get list of active subjects for course association")
    public ResponseEntity<List<SubjectResponse>> getActiveSubjects() {
        List<SubjectResponse> subjects = subjectRepository.findByStatus(SubjectStatus.ACTIVE).stream()
                .map(s -> SubjectResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .code(s.getCode())
                        .description(s.getDescription())
                        .status(s.getStatus())
                        .courseCount(0)
                        .createdAt(s.getCreatedAt())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(subjects);
    }
}
