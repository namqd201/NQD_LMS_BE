package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.student.*;

import java.util.List;
import java.util.UUID;

public interface StudentAiTutorService {

    List<AiTutorConversationDto> getConversations(UUID studentId);

    AiTutorConversationDetailDto getConversationDetail(UUID conversationId, UUID studentId);

    AiTutorConversationDto createConversation(CreateAiTutorConversationRequest request, UUID studentId);

    AiTutorConversationDto renameConversation(UUID conversationId, String newTitle, UUID studentId);

    void deleteConversation(UUID conversationId, UUID studentId);

    StudentAiTutorResponse askAiTutor(StudentAiTutorRequest request, UUID studentId);

    StudentAiTutorResponse explainLesson(UUID lessonId, String customQuestion, UUID studentId);

    StudentAiTutorResponse explainWrongAnswer(UUID examAttemptId, UUID questionId, UUID studentId);

    StudentAiTutorResponse provideHint(UUID examAttemptId, UUID questionId, UUID studentId);

    StudentAiTutorResponse getStudyRecommendations(UUID courseId, UUID studentId);
}
