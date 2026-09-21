package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.AiJobStatus;
import com.nqd.nqd_lms_be.entity.enums.AiJobType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_generation_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class AiGenerationJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private AiJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AiJobStatus status = AiJobStatus.PENDING;

    @Column(name = "prompt_summary", columnDefinition = "TEXT")
    private String promptSummary;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "total_requested", nullable = false)
    @Builder.Default
    private Integer totalRequested = 0;

    @Column(name = "total_generated", nullable = false)
    @Builder.Default
    private Integer totalGenerated = 0;

    @Column(name = "total_approved", nullable = false)
    @Builder.Default
    private Integer totalApproved = 0;

    @Column(name = "target_exam_title", length = 255)
    private String targetExamTitle;

    @Column(name = "target_exam_code", length = 50)
    private String targetExamCode;

    @Column(name = "target_exam_duration")
    private Integer targetExamDuration;

    @Column(name = "target_exam_passing_marks", precision = 7, scale = 2)
    private BigDecimal targetExamPassingMarks;

    @Column(name = "target_exam_total_marks", precision = 7, scale = 2)
    private BigDecimal targetExamTotalMarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_exam_id")
    private Exam createdExam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private QuestionCategory category;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<AiGeneratedQuestion> generatedQuestions = new ArrayList<>();
}
