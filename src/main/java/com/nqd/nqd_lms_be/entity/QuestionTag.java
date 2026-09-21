package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "question_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class QuestionTag extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
}
