package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.UUID;

@Entity
@Table(name = "question_tag_relations")
@IdClass(QuestionTagRelationId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class QuestionTagRelation extends BaseIdEntity {

    @Id
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Id
    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private QuestionTag tag;
}
