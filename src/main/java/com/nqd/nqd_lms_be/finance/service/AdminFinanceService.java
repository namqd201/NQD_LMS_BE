package com.nqd.nqd_lms_be.finance.service;

import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminFinanceService {

    AdminPlatformRevenueResponse getPlatformRevenueSummary();

    Page<TeacherEarningResponse> getAllEarnings(EarningStatus status, Pageable pageable);

    Page<TeacherWithdrawalResponse> getWithdrawals(WithdrawalStatus status, Pageable pageable);

    TeacherWithdrawalResponse approveWithdrawal(UUID withdrawalId, String adminEmail);

    TeacherWithdrawalResponse rejectWithdrawal(UUID withdrawalId, AdminProcessWithdrawalRequest request, String adminEmail);

    TeacherWithdrawalResponse completeWithdrawal(UUID withdrawalId, AdminProcessWithdrawalRequest request, String adminEmail);

    RefundResponse processRefund(ProcessRefundRequest request, String adminEmail);
}