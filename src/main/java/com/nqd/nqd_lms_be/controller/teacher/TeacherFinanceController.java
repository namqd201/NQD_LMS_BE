package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import com.nqd.nqd_lms_be.finance.service.TeacherFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Finance", description = "Endpoints for teacher earnings, balance summary, bank accounts, and payout withdrawals")
public class TeacherFinanceController {

    private final TeacherFinanceService teacherFinanceService;

    @GetMapping("/balance")
    @Operation(summary = "Get current balance summary (total earned, available, pending, withdrawn)")
    public ResponseEntity<ApiResponse<TeacherBalanceSummaryResponse>> getBalance(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherBalanceSummaryResponse response = teacherFinanceService.getBalanceSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Lay thong tin so du thanh cong", response));
    }

    @GetMapping("/earnings")
    @Operation(summary = "Get list of teacher course sale earnings with optional status filter")
    public ResponseEntity<ApiResponse<Page<TeacherEarningResponse>>> getEarnings(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) EarningStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TeacherEarningResponse> response = teacherFinanceService.getEarnings(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lay danh sach thu nhap thanh cong", response));
    }

    @GetMapping("/bank-accounts")
    @Operation(summary = "Get list of saved bank accounts for payout")
    public ResponseEntity<ApiResponse<List<TeacherBankAccountResponse>>> getBankAccounts(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        List<TeacherBankAccountResponse> response = teacherFinanceService.getBankAccounts(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Lay danh sach tai khoan ngan hang thanh cong", response));
    }

    @PostMapping("/bank-accounts")
    @Operation(summary = "Add a new bank account for payout")
    public ResponseEntity<ApiResponse<TeacherBankAccountResponse>> addBankAccount(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TeacherBankAccountRequest request
    ) {
        TeacherBankAccountResponse response = teacherFinanceService.addBankAccount(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Them tai khoan ngan hang thanh cong", response));
    }

    @DeleteMapping("/bank-accounts/{id}")
    @Operation(summary = "Delete a saved bank account")
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id
    ) {
        teacherFinanceService.deleteBankAccount(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Xoa tai khoan ngan hang thanh cong", null));
    }

    @PutMapping("/bank-accounts/{id}/default")
    @Operation(summary = "Set a bank account as default")
    public ResponseEntity<ApiResponse<TeacherBankAccountResponse>> setDefaultBankAccount(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id
    ) {
        TeacherBankAccountResponse response = teacherFinanceService.setDefaultBankAccount(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Dat tai khoan ngan hang mac dinh thanh cong", response));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Get list of payout withdrawal requests")
    public ResponseEntity<ApiResponse<Page<TeacherWithdrawalResponse>>> getWithdrawals(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 10, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TeacherWithdrawalResponse> response = teacherFinanceService.getWithdrawals(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lay lich su rut tien thanh cong", response));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Create a payout withdrawal request")
    public ResponseEntity<ApiResponse<TeacherWithdrawalResponse>> requestWithdrawal(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        TeacherWithdrawalResponse response = teacherFinanceService.requestWithdrawal(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tao yeu cau rut tien thanh cong", response));
    }

    @PostMapping("/withdrawals/{id}/cancel")
    @Operation(summary = "Cancel a pending payout withdrawal request")
    public ResponseEntity<ApiResponse<TeacherWithdrawalResponse>> cancelWithdrawal(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id
    ) {
        TeacherWithdrawalResponse response = teacherFinanceService.cancelWithdrawal(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Huy yeu cau rut tien thanh cong", response));
    }
}