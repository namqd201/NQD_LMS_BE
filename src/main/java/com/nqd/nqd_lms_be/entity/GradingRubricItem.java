package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Entity
@Table(
    name = "grading_rubric_items",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rubric_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class GradingRubricItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private GradingRubric rubric;

    @Column(name = "criterion", nullable = false, length = 255)
    private String criterion;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxPoints;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
