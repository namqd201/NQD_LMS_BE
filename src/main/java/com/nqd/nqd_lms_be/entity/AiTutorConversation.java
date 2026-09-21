package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_tutor_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTutorConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 50)
    private StudentAiTutorMode mode;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "exam_attempt_id")
    private UUID examAttemptId;

    @Column(name = "question_id")
    private UUID questionId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AiTutorMessage> messages = new ArrayList<>();
}
