package com.nqd.nqd_lms_be.service.student;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.StudentAiContextAssembler;
import com.nqd.nqd_lms_be.ai.StudentAiRateLimiter;
import com.nqd.nqd_lms_be.ai.StudentAiTutorEngine;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.entity.AiTutorConversation;
import com.nqd.nqd_lms_be.entity.AiTutorMessage;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.AiTutorConversationRepository;
import com.nqd.nqd_lms_be.repository.AiTutorMessageRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentAiTutorServiceImpl implements StudentAiTutorService {

    private final StudentAiRateLimiter rateLimiter;
    private final StudentAiContextAssembler contextAssembler;
    private final StudentAiTutorEngine tutorEngine;
    private final AiTutorConversationRepository conversationRepository;
    private final AiTutorMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final com.nqd.nqd_lms_be.repository.UserRoleRepository userRoleRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    @Transactional(readOnly = true)
    public List<AiTutorConversationDto> getConversations(UUID studentId) {
        List<AiTutorConversation> list = conversationRepository.findAllByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(studentId);
        return list.stream().map(c -> {
            String preview = "";
            int count = c.getMessages() != null ? c.getMessages().size() : 0;
            if (c.getMessages() != null && !c.getMessages().isEmpty()) {
                AiTutorMessage last = c.getMessages().get(c.getMessages().size() - 1);
                preview = last.getContent() != null ?
                        (last.getContent().length() > 80 ? last.getContent().substring(0, 80) + "..." : last.getContent()) : "";
            }
            return AiTutorConversationDto.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .mode(c.getMode())
                    .courseId(c.getCourseId())
                    .lessonId(c.getLessonId())
                    .examAttemptId(c.getExamAttemptId())
                    .questionId(c.getQuestionId())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .messageCount(count)
                    .lastMessagePreview(preview)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AiTutorConversationDetailDto getConversationDetail(UUID conversationId, UUID studentId) {
        AiTutorConversation conv = conversationRepository.findByIdAndUserIdAndIsActiveTrue(conversationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện hoặc bạn không có quyền truy cập."));

        List<AiTutorMessage> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        List<AiTutorMessageDto> messageDtos = messages.stream().map(this::mapToMessageDto).collect(Collectors.toList());

        return AiTutorConversationDetailDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .mode(conv.getMode())
                .courseId(conv.getCourseId())
                .lessonId(conv.getLessonId())
                .examAttemptId(conv.getExamAttemptId())
                .questionId(conv.getQuestionId())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .messages(messageDtos)
                .build();
    }

    @Override
    @Transactional
    public AiTutorConversationDto createConversation(CreateAiTutorConversationRequest request, UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng."));

        AiTutorConversation conv = AiTutorConversation.builder()
                .user(user)
                .title(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : "Cuộc trò chuyện mới")
                .mode(request.getMode() != null ? request.getMode() : StudentAiTutorMode.GENERAL_QA)
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .examAttemptId(request.getExamAttemptId())
                .questionId(request.getQuestionId())
                .build();

        AiTutorConversation saved = conversationRepository.save(conv);

        // Add welcome assistant message
        AiTutorMessage welcome = AiTutorMessage.builder()
                .conversation(saved)
                .role("ASSISTANT")
                .content("👋 Chào bạn! Mình là **NQD AI Tutor** - Gia sư học tập thông minh. Mình có thể hỗ trợ bạn giải đáp bài học, hướng dẫn giải bài tập và ôn luyện kiến thức. Bạn cần mình giúp gì hôm nay?")
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(welcome);

        return AiTutorConversationDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .mode(saved.getMode())
                .courseId(saved.getCourseId())
                .lessonId(saved.getLessonId())
                .examAttemptId(saved.getExamAttemptId())
                .questionId(saved.getQuestionId())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .messageCount(1)
                .lastMessagePreview(welcome.getContent())
                .build();
    }

    @Override
    @Transactional
    public AiTutorConversationDto renameConversation(UUID conversationId, String newTitle, UUID studentId) {
        AiTutorConversation conv = conversationRepository.findByIdAndUserIdAndIsActiveTrue(conversationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện."));

        conv.setTitle(newTitle != null && !newTitle.isBlank() ? newTitle.trim() : "Cuộc trò chuyện");
        AiTutorConversation saved = conversationRepository.save(conv);

        return AiTutorConversationDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .mode(saved.getMode())
                .courseId(saved.getCourseId())
                .lessonId(saved.getLessonId())
                .examAttemptId(saved.getExamAttemptId())
                .questionId(saved.getQuestionId())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId, UUID studentId) {
        AiTutorConversation conv = conversationRepository.findByIdAndUserIdAndIsActiveTrue(conversationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện."));

        conv.setIsActive(false);
        conversationRepository.save(conv);
    }

    @Override
    @Transactional
    public StudentAiTutorResponse askAiTutor(StudentAiTutorRequest request, UUID studentId) {
        log.info("Student {} interacting with AI Tutor in mode: {}", studentId, request.getMode());

        // 1. Membership & Usage Limit Check
        membershipEntitlementService.enforceAndConsumeUsage(
                studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.AI_TUTOR, 1
        );

        // 2. Rate Limiting Check
        rateLimiter.checkAndAcquire(studentId);

        // 2. Resolve or Create Conversation Entity
        AiTutorConversation conv;
        if (request.getConversationId() != null) {
            conv = conversationRepository.findByIdAndUserIdAndIsActiveTrue(request.getConversationId(), studentId)
                    .orElseGet(() -> createDefaultConversation(request, studentId));
        } else {
            conv = createDefaultConversation(request, studentId);
        }

        // 3. Strict Context Assembly & Security Validation
        StudentAiContextAssembler.AssembledStudentContext context = contextAssembler.assemble(request, studentId);

        // 4. Remaining requests in current window
        int remaining = rateLimiter.getRemainingRequests(studentId);

        // 4b. Resolve User Role
        String userRole = "STUDENT";
        try {
            List<String> roleNames = userRoleRepository.findRoleNamesByUserId(studentId);
            if (roleNames != null) {
                if (roleNames.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"))) {
                    userRole = "ADMIN";
                } else if (roleNames.stream().anyMatch(r -> r.equalsIgnoreCase("TEACHER") || r.equalsIgnoreCase("ROLE_TEACHER"))) {
                    userRole = "TEACHER";
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine user role for {}: {}", studentId, e.getMessage());
        }

        // 5. Generate AI Tutor Response
        StudentAiTutorResponse response = tutorEngine.generateTutorResponse(request, context, remaining, studentId, userRole);
        response.setConversationId(conv.getId());
        response.setConversationTitle(conv.getTitle());

        // 6. Persist User Message to DB
        String attachmentsJson = null;
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            try {
                List<StudentAiAttachmentDto> sanitized = request.getAttachments().stream().map(a ->
                        StudentAiAttachmentDto.builder()
                                .fileName(a.getFileName())
                                .fileType(a.getFileType())
                                .base64Data(a.getBase64Data())
                                .fileUrl(a.getFileUrl())
                                .extractedText(a.getExtractedText())
                                .build()
                ).collect(Collectors.toList());
                attachmentsJson = objectMapper.writeValueAsString(sanitized);
            } catch (Exception e) {
                log.warn("Failed to serialize attachments JSON: {}", e.getMessage());
            }
        }

        AiTutorMessage userMsg = AiTutorMessage.builder()
                .conversation(conv)
                .role("USER")
                .content(request.getQuestion())
                .attachmentsJson(attachmentsJson)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(userMsg);

        // 7. Persist Assistant Response to DB
        String recommendationsJson = null;
        if (response.getRecommendations() != null && !response.getRecommendations().isEmpty()) {
            try {
                recommendationsJson = objectMapper.writeValueAsString(response.getRecommendations());
            } catch (Exception e) {
                log.warn("Failed to serialize recommendations JSON: {}", e.getMessage());
            }
        }

        AiTutorMessage aiMsg = AiTutorMessage.builder()
                .conversation(conv)
                .role("ASSISTANT")
                .content(response.getAnswer() != null ? response.getAnswer() : (response.getHint() != null ? response.getHint() : ""))
                .recommendationsJson(recommendationsJson)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(aiMsg);

        // 8. Update conversation updatedAt and smart title if it's default
        if ("Cuộc trò chuyện mới".equalsIgnoreCase(conv.getTitle()) && request.getQuestion() != null) {
            String generatedTitle = request.getQuestion().trim();
            if (generatedTitle.length() > 40) {
                generatedTitle = generatedTitle.substring(0, 40) + "...";
            }
            conv.setTitle(generatedTitle);
        }
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        return response;
    }

    private AiTutorConversation createDefaultConversation(StudentAiTutorRequest request, UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        String title = "Cuộc trò chuyện mới";
        if (request.getQuestion() != null && !request.getQuestion().isBlank()) {
            title = request.getQuestion().trim();
            if (title.length() > 40) {
                title = title.substring(0, 40) + "...";
            }
        }

        AiTutorConversation conv = AiTutorConversation.builder()
                .user(user)
                .title(title)
                .mode(request.getMode() != null ? request.getMode() : StudentAiTutorMode.GENERAL_QA)
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .examAttemptId(request.getExamAttemptId())
                .questionId(request.getQuestionId())
                .build();

        return conversationRepository.save(conv);
    }

    private AiTutorMessageDto mapToMessageDto(AiTutorMessage m) {
        List<StudentAiAttachmentDto> atts = new ArrayList<>();
        if (m.getAttachmentsJson() != null && !m.getAttachmentsJson().isBlank()) {
            try {
                atts = objectMapper.readValue(m.getAttachmentsJson(), new TypeReference<List<StudentAiAttachmentDto>>() {});
            } catch (Exception ignored) {}
        }

        List<StudentStudyRecommendationDto> recs = new ArrayList<>();
        if (m.getRecommendationsJson() != null && !m.getRecommendationsJson().isBlank()) {
            try {
                recs = objectMapper.readValue(m.getRecommendationsJson(), new TypeReference<List<StudentStudyRecommendationDto>>() {});
            } catch (Exception ignored) {}
        }

        return AiTutorMessageDto.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .attachments(atts)
                .recommendations(recs)
                .createdAt(m.getCreatedAt())
                .build();
    }

    @Override
    public StudentAiTutorResponse explainLesson(UUID lessonId, String customQuestion, UUID studentId) {
        String query = (customQuestion != null && !customQuestion.isBlank()) ?
                customQuestion : "Hãy tóm tắt và giải thích các điểm trọng tâm của bài học này giúp mình với!";

        StudentAiTutorRequest request = StudentAiTutorRequest.builder()
                .lessonId(lessonId)
                .mode(StudentAiTutorMode.EXPLAIN_LESSON)
                .question(query)
                .build();

        return askAiTutor(request, studentId);
    }

    @Override
    public StudentAiTutorResponse explainWrongAnswer(UUID examAttemptId, UUID questionId, UUID studentId) {
        StudentAiTutorRequest request = StudentAiTutorRequest.builder()
                .examAttemptId(examAttemptId)
                .questionId(questionId)
                .mode(StudentAiTutorMode.EXPLAIN_WRONG_ANSWER)
                .question("Tại sao câu trả lời của mình lại chưa chính xác? Hãy phân tích lỗi sai và hướng dẫn cách làm đúng giúp mình nhé!")
                .build();

        return askAiTutor(request, studentId);
    }

    @Override
    public StudentAiTutorResponse provideHint(UUID examAttemptId, UUID questionId, UUID studentId) {
        StudentAiTutorRequest request = StudentAiTutorRequest.builder()
                .examAttemptId(examAttemptId)
                .questionId(questionId)
                .mode(StudentAiTutorMode.PROVIDE_HINT)
                .question("Hãy cho mình một gợi ý nhỏ hoặc câu hỏi định hướng để mình tự giải câu này nhé!")
                .build();

        return askAiTutor(request, studentId);
    }

    @Override
    public StudentAiTutorResponse getStudyRecommendations(UUID courseId, UUID studentId) {
        StudentAiTutorRequest request = StudentAiTutorRequest.builder()
                .courseId(courseId)
                .mode(StudentAiTutorMode.RECOMMEND_STUDY)
                .question("Dựa trên kết quả học tập của mình, mình nên ưu tiên ôn tập hoặc học tiếp những nội dung nào?")
                .build();

        return askAiTutor(request, studentId);
    }
}
