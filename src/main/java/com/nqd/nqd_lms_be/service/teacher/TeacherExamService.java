package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;

import java.util.List;
import java.util.UUID;

public interface TeacherExamService {
    List<TeacherExamResponse> getExams(
            UUID subjectId,
            UUID courseId,
            String gradeLevel,
            ExamStatus status,
            String keyword,
            UUID teacherId
    );

    TeacherExamResponse getExamById(UUID examId, UUID teacherId);

    TeacherExamResponse createExam(TeacherExamRequest request, UUID teacherId);

    TeacherExamResponse updateExam(UUID examId, TeacherExamRequest request, UUID teacherId);

    TeacherExamResponse publishExam(UUID examId, UUID teacherId);

    TeacherExamResponse archiveExam(UUID examId, UUID teacherId);

    void deleteExam(UUID examId, UUID teacherId);

    List<TeacherExamResponse> getDeletedExams(UUID teacherId);

    TeacherExamResponse restoreExam(UUID examId, UUID teacherId);

    TeacherBlueprintResponse createOrUpdateBlueprint(UUID examId, TeacherBlueprintRequest request, UUID teacherId);

    TeacherBlueprintResponse getBlueprint(UUID examId, UUID teacherId);

    void gradeAttempt(UUID attemptId, TeacherGradeAttemptRequest request, UUID teacherId);

    List<TeacherExamStudentCandidateResponse> getEligibleStudentsForExam(UUID examId, UUID teacherId);

    void assignStudentsToExam(UUID examId, AssignStudentsToExamRequest request, UUID teacherId);

    TeacherExamResultsSummaryResponse getExamResults(UUID examId, UUID teacherId);

    TeacherExamAttemptDetailResponse getAttemptDetail(UUID attemptId, UUID teacherId);

    TeacherExamResponse cloneExam(UUID examId, UUID teacherId);

    List<TeacherExamResponse> getSharedExamLibrary(UUID subjectId, String gradeLevel, String keyword, UUID teacherId);

    TeacherExamResponse updateExamVisibility(UUID examId, ExamVisibility visibility, UUID teacherId);
}
