package com.nqd.nqd_lms_be.finance.service;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFinanceServiceImpl implements AdminFinanceService {

    private final TeacherEarningRepository teacherEarningRepository;
    private final TeacherWithdrawalRepository teacherWithdrawalRepository;
    private final OrderRepository orderRepository;
    private final EntitlementRepository entitlementRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminPlatformRevenueResponse getPlatformRevenueSummary() {
        BigDecimal grossRevenue = teacherEarningRepository.sumPlatformGrossRevenue();
        BigDecimal platformTotalFees = teacherEarningRepository.sumPlatformTotalFees();
        BigDecimal teacherTotalEarnings = teacherEarningRepository.sumPlatformTotalTeacherPayouts();
        BigDecimal completedWithdrawals = teacherWithdrawalRepository.sumPlatformTotalCompletedWithdrawals();
        BigDecimal pendingWithdrawals = teacherWithdrawalRepository.sumPlatformTotalPendingWithdrawals();
        BigDecimal reversedAmount = teacherEarningRepository.sumPlatformReversedAmount();

        return AdminPlatformRevenueResponse.builder()
                .grossRevenue(grossRevenue)
                .platformTotalFees(platformTotalFees)
                .teacherTotalEarnings(teacherTotalEarnings)
                .completedWithdrawals(completedWithdrawals)
                .pendingWithdrawals(pendingWithdrawals)
                .reversedAmount(reversedAmount)
                .currency("VND")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherEarningResponse> getAllEarnings(EarningStatus status, Pageable pageable) {
        Page<TeacherEarning> page;
        if (status != null) {
            page = teacherEarningRepository.findByTeacherIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(null, status, pageable);
        } else {
            page = teacherEarningRepository.findAll(pageable);
        }
        return page.map(this::toEarningResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherWithdrawalResponse> getWithdrawals(WithdrawalStatus status, Pageable pageable) {
        Page<TeacherWithdrawal> page;
        if (status != null) {
            page = teacherWithdrawalRepository.findByStatusAndIsDeletedFalseOrderByRequestedAtDesc(status, pageable);
        } else {
            page = teacherWithdrawalRepository.findAllByIsDeletedFalseOrderByRequestedAtDesc(pageable);
        }
        return page.map(this::toWithdrawalResponse);
    }

    @Override
    @Transactional
    public TeacherWithdrawalResponse approveWithdrawal(UUID withdrawalId, String adminEmail) {
        TeacherWithdrawal withdrawal = teacherWithdrawalRepository.findById(withdrawalId)
                .filter(w -> !w.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut tien"));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chi co the duyet yeu cau rut tien dang o trang thai PENDING");
        }

        withdrawal.setStatus(WithdrawalStatus.PROCESSING);
        withdrawal.setProcessedBy(adminEmail);
        withdrawal.setProcessedAt(LocalDateTime.now());

        TeacherWithdrawal saved = teacherWithdrawalRepository.save(withdrawal);
        log.info("Admin {} approved withdrawal {}: code={}", adminEmail, withdrawalId, saved.getWithdrawalCode());
        return toWithdrawalResponse(saved);
    }

    @Override
    @Transactional
    public TeacherWithdrawalResponse rejectWithdrawal(UUID withdrawalId, AdminProcessWithdrawalRequest request, String adminEmail) {
        TeacherWithdrawal withdrawal = teacherWithdrawalRepository.findById(withdrawalId)
                .filter(w -> !w.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut tien"));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING && withdrawal.getStatus() != WithdrawalStatus.PROCESSING) {
            throw new IllegalStateException("Khong the tu choi yeu cau rut tien o trang thai " + withdrawal.getStatus());
        }

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(request != null ? request.getRejectionReason() : null);
        withdrawal.setProcessedBy(adminEmail);
        withdrawal.setProcessedAt(LocalDateTime.now());

        TeacherWithdrawal saved = teacherWithdrawalRepository.save(withdrawal);
        log.info("Admin {} rejected withdrawal {}: code={}, reason={}", adminEmail, withdrawalId, saved.getWithdrawalCode(), saved.getRejectionReason());
        return toWithdrawalResponse(saved);
    }

    @Override
    @Transactional
    public TeacherWithdrawalResponse completeWithdrawal(UUID withdrawalId, AdminProcessWithdrawalRequest request, String adminEmail) {
        TeacherWithdrawal withdrawal = teacherWithdrawalRepository.findById(withdrawalId)
                .filter(w -> !w.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut tien"));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING && withdrawal.getStatus() != WithdrawalStatus.PROCESSING) {
            throw new IllegalStateException("Khong the hoan tat yeu cau rut tien o trang thai " + withdrawal.getStatus());
        }

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        if (request != null && request.getReferenceCode() != null) {
            withdrawal.setReferenceCode(request.getReferenceCode());
        }
        withdrawal.setProcessedBy(adminEmail);
        withdrawal.setProcessedAt(LocalDateTime.now());

        TeacherWithdrawal saved = teacherWithdrawalRepository.save(withdrawal);
        log.info("Admin {} completed withdrawal {}: code={}, reference={}", adminEmail, withdrawalId, saved.getWithdrawalCode(), saved.getReferenceCode());
        return toWithdrawalResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse processRefund(ProcessRefundRequest request, String adminEmail) {
        Order order = orderRepository.findById(request.getOrderId())
                .filter(o -> !o.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Chi co the hoan tien cho don hang da thanh toan thanh cong (PAID)");
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = request.getRefundReason() != null ? request.getRefundReason() : "Hoan tien boi Admin: " + adminEmail;

        // 1. Update Order status
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // 2. Reverse Teacher Earnings
        List<TeacherEarning> earnings = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId());
        for (TeacherEarning earning : earnings) {
            earning.setStatus(EarningStatus.REVERSED);
            earning.setReversedAt(now);
            earning.setReversalReason(reason);
        }
        teacherEarningRepository.saveAll(earnings);

        // 3. Revoke active entitlements granted from this order
        List<Entitlement> entitlements = entitlementRepository.findBySourceOrderIdAndIsDeletedFalse(order.getId());
        for (Entitlement entitlement : entitlements) {
            entitlement.setStatus(EntitlementStatus.EXPIRED);
            entitlement.setValidUntil(now);
        }
        entitlementRepository.saveAll(entitlements);

        // 4. Revoke course enrollments if applicable
        if (order.getUser() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null && item.getProduct().getTargetEntityId() != null) {
                    UUID courseId = item.getProduct().getTargetEntityId();
                    var enrollmentOpt = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, order.getUser().getId());
                    enrollmentOpt.ifPresent(enrollment -> {
                        enrollment.setIsDeleted(true);
                        courseEnrollmentRepository.save(enrollment);
                    });
                }
            }
        }

        log.info("Successfully processed refund for order #{}, reversed {} earnings, revoked {} entitlements",
                order.getOrderCode(), earnings.size(), entitlements.size());

        return RefundResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .refundAmount(order.getFinalAmount())
                .status("REFUNDED")
                .reason(reason)
                .refundedAt(now)
                .reversedEarningsCount(earnings.size())
                .revokedEntitlementsCount(entitlements.size())
                .build();
    }

    private TeacherEarningResponse toEarningResponse(TeacherEarning earning) {
        return TeacherEarningResponse.builder()
                .id(earning.getId())
                .orderId(earning.getOrder() != null ? earning.getOrder().getId() : null)
                .orderCode(earning.getOrder() != null ? earning.getOrder().getOrderCode() : null)
                .orderItemId(earning.getOrderItem() != null ? earning.getOrderItem().getId() : null)
                .courseId(earning.getCourse() != null ? earning.getCourse().getId() : null)
                .courseName(earning.getCourse() != null ? earning.getCourse().getName() : null)
                .grossAmount(earning.getGrossAmount())
                .platformFeeRate(earning.getPlatformFeeRate())
                .platformFee(earning.getPlatformFee())
                .teacherShareRate(earning.getTeacherShareRate())
                .teacherAmount(earning.getTeacherAmount())
                .currency(earning.getCurrency())
                .status(earning.getStatus().name())
                .availableAt(earning.getAvailableAt())
                .reversedAt(earning.getReversedAt())
                .reversalReason(earning.getReversalReason())
                .createdAt(earning.getCreatedAt())
                .build();
    }

    private TeacherWithdrawalResponse toWithdrawalResponse(TeacherWithdrawal withdrawal) {
        return TeacherWithdrawalResponse.builder()
                .id(withdrawal.getId())
                .withdrawalCode(withdrawal.getWithdrawalCode())
                .amount(withdrawal.getAmount())
                .currency(withdrawal.getCurrency())
                .bankName(withdrawal.getBankName())
                .bankCode(withdrawal.getBankCode())
                .accountNumberMasked(withdrawal.getMaskedAccountNumber())
                .accountHolderName(withdrawal.getAccountHolderName())
                .status(withdrawal.getStatus().name())
                .referenceCode(withdrawal.getReferenceCode())
                .rejectionReason(withdrawal.getRejectionReason())
                .requestedAt(withdrawal.getRequestedAt())
                .processedAt(withdrawal.getProcessedAt())
                .processedBy(withdrawal.getProcessedBy())
                .build();
    }
}