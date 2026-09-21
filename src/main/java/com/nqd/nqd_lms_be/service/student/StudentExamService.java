package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamResponse;

import java.util.List;
import java.util.UUID;

public interface StudentExamService {
    List<TeacherExamResponse> getAvailableExams();
    List<StudentAssignedExamResponse> getMyAssignedExams(UUID studentId);
    StudentExamTakingResponse startExam(UUID examId, UUID studentId);
    StudentAttemptResultResponse submitExam(UUID attemptId, SubmitExamAttemptRequest request, UUID studentId);
    StudentAttemptResultResponse getAttemptResult(UUID attemptId, UUID studentId);
    List<StudentAttemptResultResponse> getMyExamAttempts(UUID examId, UUID studentId);
    StudentExamAttemptReviewResponse getAttemptReview(UUID attemptId, UUID studentId);
    StudentProgressResponse getStudentProgress(UUID studentId);
    ExamAttemptEventBatchResponse recordAttemptEvents(UUID attemptId, List<ExamAttemptEventRequest> requests, UUID studentId);
}
