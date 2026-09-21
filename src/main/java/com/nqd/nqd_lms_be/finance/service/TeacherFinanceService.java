package com.nqd.nqd_lms_be.finance.service;

import com.nqd.nqd_lms_be.entity.OrderItem;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TeacherFinanceService {

    void recordEarningForOrderItem(OrderItem orderItem);

    void recordUltraRoyaltyEarning(com.nqd.nqd_lms_be.entity.Course course, com.nqd.nqd_lms_be.entity.User student, java.math.BigDecimal royaltyAmount);

    TeacherBalanceSummaryResponse getBalanceSummary(UUID teacherId);

    Page<TeacherEarningResponse> getEarnings(UUID teacherId, EarningStatus status, Pageable pageable);

    List<TeacherBankAccountResponse> getBankAccounts(UUID teacherId);

    TeacherBankAccountResponse addBankAccount(UUID teacherId, TeacherBankAccountRequest request);

    void deleteBankAccount(UUID teacherId, UUID bankAccountId);

    TeacherBankAccountResponse setDefaultBankAccount(UUID teacherId, UUID bankAccountId);

    TeacherWithdrawalResponse requestWithdrawal(UUID teacherId, CreateWithdrawalRequest request);

    Page<TeacherWithdrawalResponse> getWithdrawals(UUID teacherId, Pageable pageable);

    TeacherWithdrawalResponse cancelWithdrawal(UUID teacherId, UUID withdrawalId);
}