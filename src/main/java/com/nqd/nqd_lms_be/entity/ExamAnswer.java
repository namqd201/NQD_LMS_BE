package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.GradingMethod;
import com.nqd.nqd_lms_be.entity.enums.GradingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exam_answers",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"attempt_id", "question_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "max_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "marks_awarded", precision = 7, scale = 2)
    private BigDecimal marksAwarded;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 20)
    @Builder.Default
    private GradingStatus gradingStatus = GradingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_method", length = 20)
    private GradingMethod gradingMethod;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User grader;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
}
