package com.nqd.nqd_lms_be.finance.service;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherFinanceServiceImpl implements TeacherFinanceService {

    private final TeacherEarningRepository teacherEarningRepository;
    private final TeacherBankAccountRepository teacherBankAccountRepository;
    private final TeacherWithdrawalRepository teacherWithdrawalRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Value("${app.finance.platform-fee-rate:0.20}")
    private BigDecimal defaultPlatformFeeRate;

    @Value("${app.finance.hold-period-days:7}")
    private int holdPeriodDays;

    @Override
    @Transactional
    public void recordEarningForOrderItem(OrderItem orderItem) {
        if (orderItem == null || orderItem.getProductType() == null || !orderItem.getProductType().isEducationalContent()) {
            return;
        }

        if (teacherEarningRepository.findByOrderItemIdAndIsDeletedFalse(orderItem.getId()).isPresent()) {
            log.info("TeacherEarning already recorded for orderItemId: {}", orderItem.getId());
            return;
        }

        UUID targetEntityId = orderItem.getProduct() != null ? orderItem.getProduct().getTargetEntityId() : null;
        if (targetEntityId == null) {
            log.warn("Cannot record earning: Product has no targetEntityId for order item {}", orderItem.getId());
            return;
        }

        Course course = null;
        if (orderItem.getProductType().isCourse()) {
            course = courseRepository.findById(targetEntityId).orElse(null);
        } else if (orderItem.getProductType().isChapter()) {
            Chapter ch = chapterRepository.findById(targetEntityId).orElse(null);
            if (ch != null) course = ch.getCourse();
        } else if (orderItem.getProductType().isLesson()) {
            Lesson l = lessonRepository.findById(targetEntityId).orElse(null);
            if (l != null && l.getChapter() != null) course = l.getChapter().getCourse();
        }

        if (course == null) {
            log.warn("Cannot record earning: Course not found for order item {}", orderItem.getId());
            return;
        }

        User teacher = course.getCreator();
        if (teacher == null) {
            List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(course.getId());
            if (!teachers.isEmpty()) {
                teacher = teachers.get(0).getTeacher();
            }
        }

        if (teacher == null) {
            log.warn("Cannot record earning: No teacher associated with course {}", course.getId());
            return;
        }

        BigDecimal grossAmount = orderItem.getSubtotal() != null ? orderItem.getSubtotal() : BigDecimal.ZERO;
        if (grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        boolean isBuyerPro = false;
        if (orderItem.getOrder() != null && orderItem.getOrder().getUser() != null) {
            UUID buyerId = orderItem.getOrder().getUser().getId();
            isBuyerPro = membershipEntitlementService.hasFeature(buyerId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.DISCOUNT_ON_PURCHASES);
        }

        // COURSERA-STYLE PRO FINANCIAL MODEL:
        // If buyer is PRO, Admin charges 0% platform fee -> Teacher gets 100% of the discounted purchase amount!
        BigDecimal platformFeeRate = isBuyerPro ? BigDecimal.ZERO : (defaultPlatformFeeRate != null ? defaultPlatformFeeRate : new BigDecimal("0.20"));
        BigDecimal teacherShareRate = BigDecimal.ONE.subtract(platformFeeRate);

        BigDecimal platformFee = grossAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal teacherAmount = grossAmount.subtract(platformFee);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime availableAt = holdPeriodDays > 0 ? now.plusDays(holdPeriodDays) : now;
        EarningStatus status = holdPeriodDays > 0 ? EarningStatus.PENDING : EarningStatus.AVAILABLE;

        TeacherEarning earning = TeacherEarning.builder()
                .order(orderItem.getOrder())
                .orderItem(orderItem)
                .course(course)
                .teacher(teacher)
                .grossAmount(grossAmount)
                .platformFeeRate(platformFeeRate)
                .platformFee(platformFee)
                .teacherShareRate(teacherShareRate)
                .teacherAmount(teacherAmount)
                .currency(orderItem.getCurrency() != null ? orderItem.getCurrency() : "VND")
                .status(status)
                .availableAt(availableAt)
                .build();

        teacherEarningRepository.save(earning);
        log.info("Successfully recorded TeacherEarning for teacher: {}, course: {}, gross: {}, teacherAmount: {}, availableAt: {}",
                teacher.getId(), course.getId(), grossAmount, teacherAmount, availableAt);
    }

    @Override
    @Transactional
    public void recordUltraRoyaltyEarning(Course course, User student, BigDecimal royaltyAmount) {
        if (course == null || royaltyAmount == null || royaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        User teacher = course.getCreator();
        if (teacher == null) {
            List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(course.getId());
            if (!teachers.isEmpty()) {
                teacher = teachers.get(0).getTeacher();
            }
        }

        if (teacher == null) {
            log.warn("Cannot record Ultra royalty: No teacher associated with course {}", course.getId());
            return;
        }

        TeacherEarning earning = TeacherEarning.builder()
                .course(course)
                .teacher(teacher)
                .grossAmount(royaltyAmount)
                .platformFeeRate(BigDecimal.ZERO)
                .platformFee(BigDecimal.ZERO)
                .teacherShareRate(BigDecimal.ONE)
                .teacherAmount(royaltyAmount)
                .currency(course.getCurrency() != null ? course.getCurrency() : "VND")
                .status(EarningStatus.AVAILABLE)
                .availableAt(LocalDateTime.now())
                .build();

        teacherEarningRepository.save(earning);
        log.info("Successfully recorded Ultra Royalty Earning: teacher: {}, course: {}, amount: {}",
                teacher.getId(), course.getId(), royaltyAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherBalanceSummaryResponse getBalanceSummary(UUID teacherId) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalEarned = teacherEarningRepository.sumTotalEarnedByTeacherId(teacherId);
        BigDecimal availableEarned = teacherEarningRepository.sumAvailableEarningByTeacherId(teacherId, now);
        BigDecimal pendingBalance = teacherEarningRepository.sumPendingEarningByTeacherId(teacherId, now);
        BigDecimal committedWithdrawals = teacherWithdrawalRepository.sumCommittedWithdrawalsByTeacherId(teacherId);
        BigDecimal availableBalance = availableEarned.subtract(committedWithdrawals).max(BigDecimal.ZERO);
        BigDecimal withdrawnAmount = teacherWithdrawalRepository.sumCompletedWithdrawalsByTeacherId(teacherId);
        BigDecimal reversedAmount = teacherEarningRepository.sumReversedEarningByTeacherId(teacherId);

        return TeacherBalanceSummaryResponse.builder()
                .totalEarned(totalEarned)
                .availableBalance(availableBalance)
                .pendingBalance(pendingBalance)
                .withdrawnAmount(withdrawnAmount)
                .reversedAmount(reversedAmount)
                .currency("VND")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherEarningResponse> getEarnings(UUID teacherId, EarningStatus status, Pageable pageable) {
        Page<TeacherEarning> page;
        if (status != null) {
            page = teacherEarningRepository.findByTeacherIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(teacherId, status, pageable);
        } else {
            page = teacherEarningRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacherId, pageable);
        }
        return page.map(this::toEarningResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherBankAccountResponse> getBankAccounts(UUID teacherId) {
        return teacherBankAccountRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(this::toBankAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherBankAccountResponse addBankAccount(UUID teacherId, TeacherBankAccountRequest request) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thong tin giang vien"));

        List<TeacherBankAccount> existingAccounts = teacherBankAccountRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacherId);
        boolean isFirst = existingAccounts.isEmpty();
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || isFirst;

        if (makeDefault && !existingAccounts.isEmpty()) {
            for (TeacherBankAccount acc : existingAccounts) {
                acc.setIsDefault(false);
            }
            teacherBankAccountRepository.saveAll(existingAccounts);
        }

        TeacherBankAccount bankAccount = TeacherBankAccount.builder()
                .teacher(teacher)
                .bankName(request.getBankName())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .isDefault(makeDefault)
                .isVerified(true)
                .build();

        TeacherBankAccount saved = teacherBankAccountRepository.save(bankAccount);
        return toBankAccountResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBankAccount(UUID teacherId, UUID bankAccountId) {
        TeacherBankAccount account = teacherBankAccountRepository.findByIdAndTeacherIdAndIsDeletedFalse(bankAccountId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan ngan hang"));

        account.setIsDeleted(true);
        teacherBankAccountRepository.save(account);

        if (Boolean.TRUE.equals(account.getIsDefault())) {
            List<TeacherBankAccount> remaining = teacherBankAccountRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacherId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(true);
                teacherBankAccountRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public TeacherBankAccountResponse setDefaultBankAccount(UUID teacherId, UUID bankAccountId) {
        TeacherBankAccount target = teacherBankAccountRepository.findByIdAndTeacherIdAndIsDeletedFalse(bankAccountId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan ngan hang"));

        List<TeacherBankAccount> allAccounts = teacherBankAccountRepository.findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacherId);
        for (TeacherBankAccount acc : allAccounts) {
            acc.setIsDefault(acc.getId().equals(bankAccountId));
        }
        teacherBankAccountRepository.saveAll(allAccounts);

        return toBankAccountResponse(target);
    }

    @Override
    @Transactional
    public TeacherWithdrawalResponse requestWithdrawal(UUID teacherId, CreateWithdrawalRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = teacherWithdrawalRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return toWithdrawalResponse(existing.get());
            }
        }

        User teacher = userRepository.findByIdWithLock(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thong tin giang vien"));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal availableEarned = teacherEarningRepository.sumAvailableEarningByTeacherId(teacherId, now);
        BigDecimal committedWithdrawals = teacherWithdrawalRepository.sumCommittedWithdrawalsByTeacherId(teacherId);
        BigDecimal availableBalance = availableEarned.subtract(committedWithdrawals).max(BigDecimal.ZERO);

        if (request.getAmount() == null || request.getAmount().compareTo(availableBalance) > 0) {
            throw new IllegalArgumentException("So du kha dung khong du de thuc hien rut tien. Kha dung: " +
                    availableBalance + " VND, yeu cau: " + request.getAmount() + " VND");
        }

        String bankName = request.getBankName();
        String bankCode = request.getBankCode();
        String accountNumber = request.getAccountNumber();
        String accountHolderName = request.getAccountHolderName();

        if (request.getBankAccountId() != null) {
            TeacherBankAccount bankAccount = teacherBankAccountRepository.findByIdAndTeacherIdAndIsDeletedFalse(request.getBankAccountId(), teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan ngan hang da chon"));
            bankName = bankAccount.getBankName();
            bankCode = bankAccount.getBankCode();
            accountNumber = bankAccount.getAccountNumber();
            accountHolderName = bankAccount.getAccountHolderName();
        } else if (accountNumber == null || accountNumber.isBlank()) {
            TeacherBankAccount defaultAccount = teacherBankAccountRepository.findByTeacherIdAndIsDefaultTrueAndIsDeletedFalse(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Vui long chon hoac cung cap thong tin tai khoan ngan hang nhan tien."));
            bankName = defaultAccount.getBankName();
            bankCode = defaultAccount.getBankCode();
            accountNumber = defaultAccount.getAccountNumber();
            accountHolderName = defaultAccount.getAccountHolderName();
        }

        String withdrawalCode = "WTD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TeacherWithdrawal withdrawal = TeacherWithdrawal.builder()
                .withdrawalCode(withdrawalCode)
                .teacher(teacher)
                .amount(request.getAmount())
                .currency("VND")
                .bankName(bankName)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .status(WithdrawalStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .requestedAt(now)
                .build();

        TeacherWithdrawal saved = teacherWithdrawalRepository.save(withdrawal);
        log.info("Created withdrawal request: code={}, teacher={}, amount={}", withdrawalCode, teacherId, request.getAmount());
        return toWithdrawalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherWithdrawalResponse> getWithdrawals(UUID teacherId, Pageable pageable) {
        return teacherWithdrawalRepository.findByTeacherIdAndIsDeletedFalseOrderByRequestedAtDesc(teacherId, pageable)
                .map(this::toWithdrawalResponse);
    }

    @Override
    @Transactional
    public TeacherWithdrawalResponse cancelWithdrawal(UUID teacherId, UUID withdrawalId) {
        TeacherWithdrawal withdrawal = teacherWithdrawalRepository.findById(withdrawalId)
                .filter(w -> !w.getIsDeleted() && w.getTeacher().getId().equals(teacherId))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut tien"));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chi co the huy yeu cau rut tien dang o trang thai PENDING");
        }

        withdrawal.setStatus(WithdrawalStatus.CANCELLED);
        TeacherWithdrawal saved = teacherWithdrawalRepository.save(withdrawal);
        log.info("Cancelled withdrawal request: code={}, teacher={}", withdrawal.getWithdrawalCode(), teacherId);
        return toWithdrawalResponse(saved);
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

    private TeacherBankAccountResponse toBankAccountResponse(TeacherBankAccount account) {
        return TeacherBankAccountResponse.builder()
                .id(account.getId())
                .bankName(account.getBankName())
                .bankCode(account.getBankCode())
                .accountNumberMasked(account.getMaskedAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .isDefault(account.getIsDefault())
                .isVerified(account.getIsVerified())
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