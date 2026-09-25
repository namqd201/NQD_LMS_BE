package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "classrooms",
    indexes = {
        @Index(name = "idx_classrooms_code", columnList = "code", unique = true),
        @Index(name = "idx_classrooms_teacher", columnList = "teacher_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Classroom extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /**
     * Mỗi lớp chỉ có 1 giáo viên phụ trách (Chủ nhiệm / Quản lý).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ClassroomStatus status = ClassroomStatus.ACTIVE;

    @Column(name = "student_count", nullable = false)
    @Builder.Default
    private Integer studentCount = 0;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "lark_meeting_url", columnDefinition = "TEXT")
    private String larkMeetingUrl;

    @Column(name = "meeting_id", length = 100)
    private String meetingId;

    @Column(name = "passcode", length = 100)
    private String passcode;

    @Column(name = "meeting_note", columnDefinition = "TEXT")
    private String meetingNote;

    @Column(name = "is_live_now", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isLiveNow = false;
}
