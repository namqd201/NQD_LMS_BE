package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.*;

import java.util.List;
import java.util.UUID;

public interface TeacherAiGenerationService {
    TeacherAiJobDetailResponse generateQuestions(TeacherAiGenerateQuestionsRequest request, UUID teacherId);
    TeacherAiJobDetailResponse generateExam(TeacherAiGenerateExamRequest request, UUID teacherId);
    List<TeacherAiJobResponse> getMyJobs(UUID teacherId);
    TeacherAiJobDetailResponse getJobDetail(UUID jobId, UUID teacherId);
    TeacherAiGeneratedQuestionResponse updateGeneratedQuestion(UUID jobId, UUID questionId, TeacherAiUpdateGeneratedQuestionRequest request, UUID teacherId);
    TeacherAiGeneratedQuestionResponse approveSingleQuestion(UUID jobId, UUID questionId, UUID teacherId);
    TeacherAiGeneratedQuestionResponse rejectSingleQuestion(UUID jobId, UUID questionId, UUID teacherId);
    TeacherAiApproveJobResponse approveAllValidQuestions(UUID jobId, UUID teacherId);
    TeacherAiApproveJobResponse createExamFromJob(UUID jobId, UUID teacherId);
    void deleteJob(UUID jobId, UUID teacherId);
}
