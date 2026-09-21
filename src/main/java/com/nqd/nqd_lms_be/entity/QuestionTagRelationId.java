package com.nqd.nqd_lms_be.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuestionTagRelationId implements Serializable {
    private UUID questionId;
    private UUID tagId;
}
