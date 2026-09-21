package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.DocumentFormat;
import com.nqd.nqd_lms_be.entity.enums.DocumentStatus;
import com.nqd.nqd_lms_be.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "generated_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class GeneratedDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_version_id")
    private ExamVersion examVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private DocumentFormat format;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "storage_key", columnDefinition = "TEXT")
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.GENERATING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
