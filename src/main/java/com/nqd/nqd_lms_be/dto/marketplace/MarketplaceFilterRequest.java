package com.nqd.nqd_lms_be.dto.marketplace;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceFilterRequest {
    private String keyword;
    private UUID subjectId;
    private String gradeLevel;
    private UUID teacherId;
    private Boolean isFree;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private String sortBy; // newest, popular, rating, price_asc, price_desc
}
