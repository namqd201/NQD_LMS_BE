package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "exam_versions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "version_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "version_code", nullable = false, length = 20)
    private String versionCode;

    @Column(name = "version_name", length = 100)
    private String versionName;
}
