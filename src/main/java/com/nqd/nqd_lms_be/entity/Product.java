package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_code", columnList = "code"),
        @Index(name = "idx_products_type_status", columnList = "product_type, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Product extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private ProductType productType;

    /**
     * Target Entity ID (e.g. Course ID, Exam ID, etc.) for direct single-item purchases.
     */
    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User creator;
}
