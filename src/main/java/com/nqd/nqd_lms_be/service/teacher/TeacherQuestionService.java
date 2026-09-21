package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionResponse;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;

import java.util.List;
import java.util.UUID;

public interface TeacherQuestionService {
    List<TeacherQuestionResponse> getQuestions(
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
            UUID teacherId
    );

    TeacherQuestionResponse getQuestionById(UUID questionId, UUID teacherId);

    TeacherQuestionResponse createQuestion(TeacherQuestionRequest request, UUID teacherId);

    TeacherQuestionResponse updateQuestion(UUID questionId, TeacherQuestionRequest request, UUID teacherId);

    TeacherQuestionResponse updateQuestionStatus(UUID questionId, QuestionStatus status, UUID teacherId);

    TeacherQuestionResponse archiveQuestion(UUID questionId, UUID teacherId);

    void deleteQuestion(UUID questionId, UUID teacherId);

    List<TeacherQuestionResponse> getDeletedQuestions(UUID teacherId);

    TeacherQuestionResponse restoreQuestion(UUID questionId, UUID teacherId);
}
