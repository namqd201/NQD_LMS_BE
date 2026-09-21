package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.Subscription;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private UUID planId;
    private String planCode;
    private String planName;
    private MembershipPlanResponse plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenew;
    private LocalDateTime cancelledAt;
    private UUID sourceOrderId;
    private String sourceOrderCode;
    private boolean isCurrentlyActive;
    private boolean isVip;
    private LocalDateTime createdAt;

    public static SubscriptionResponse fromEntity(Subscription sub) {
        if (sub == null) return null;
        boolean vip = sub.isCurrentlyActive() && sub.getMembershipPlan() != null
                && sub.getMembershipPlan().getPlanCode() != null
                && !sub.getMembershipPlan().getPlanCode().toUpperCase().contains("FREE");
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUser() != null ? sub.getUser().getId() : null)
                .userEmail(sub.getUser() != null ? sub.getUser().getEmail() : null)
                .userFullName(sub.getUser() != null ? sub.getUser().getFullName() : null)
                .planId(sub.getMembershipPlan() != null ? sub.getMembershipPlan().getId() : null)
                .planCode(sub.getMembershipPlan() != null ? sub.getMembershipPlan().getPlanCode() : null)
                .planName(sub.getMembershipPlan() != null ? sub.getMembershipPlan().getName() : null)
                .plan(MembershipPlanResponse.fromEntity(sub.getMembershipPlan()))
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .autoRenew(sub.getAutoRenew())
                .cancelledAt(sub.getCancelledAt())
                .sourceOrderId(sub.getSourceOrder() != null ? sub.getSourceOrder().getId() : null)
                .sourceOrderCode(sub.getSourceOrder() != null ? sub.getSourceOrder().getOrderCode() : null)
                .isCurrentlyActive(sub.isCurrentlyActive())
                .isVip(vip)
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
