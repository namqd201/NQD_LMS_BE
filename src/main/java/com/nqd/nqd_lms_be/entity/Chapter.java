package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "chapters",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Chapter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "price", precision = 15, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "is_sellable", nullable = false)
    @Builder.Default
    private Boolean isSellable = false;
}
