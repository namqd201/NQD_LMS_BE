package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import com.nqd.nqd_lms_be.finance.dto.*;
import com.nqd.nqd_lms_be.finance.service.AdminFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Finance", description = "Administrative endpoints for platform revenue analytics, teacher payouts, and refund processing")
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    @GetMapping("/overview")
    @Operation(summary = "Get platform financial summary (gross revenue, platform fees, teacher payouts, pending withdrawals)")
    public ResponseEntity<ApiResponse<AdminPlatformRevenueResponse>> getOverview() {
        AdminPlatformRevenueResponse response = adminFinanceService.getPlatformRevenueSummary();
        return ResponseEntity.ok(ApiResponse.ok("Lay tong quan tai chinh he thong thanh cong", response));
    }

    @GetMapping("/earnings")
    @Operation(summary = "List all teacher earnings across the platform with optional status filter")
    public ResponseEntity<ApiResponse<Page<TeacherEarningResponse>>> getAllEarnings(
            @RequestParam(required = false) EarningStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TeacherEarningResponse> response = adminFinanceService.getAllEarnings(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lay danh sach thu nhap he thong thanh cong", response));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "List all teacher payout requests with optional status filter")
    public ResponseEntity<ApiResponse<Page<TeacherWithdrawalResponse>>> getWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @PageableDefault(size = 10, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TeacherWithdrawalResponse> response = adminFinanceService.getWithdrawals(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lay danh sach yeu cau rut tien thanh cong", response));
    }

    @PostMapping("/withdrawals/{id}/approve")
    @Operation(summary = "Approve teacher withdrawal request (Transitions to PROCESSING)")
    public ResponseEntity<ApiResponse<TeacherWithdrawalResponse>> approveWithdrawal(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String adminEmail = principal != null ? principal.getEmail() : "admin@nqd.edu.vn";
        TeacherWithdrawalResponse response = adminFinanceService.approveWithdrawal(id, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Duyet yeu cau rut tien thanh cong", response));
    }

    @PostMapping("/withdrawals/{id}/reject")
    @Operation(summary = "Reject teacher withdrawal request")
    public ResponseEntity<ApiResponse<TeacherWithdrawalResponse>> rejectWithdrawal(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminProcessWithdrawalRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String adminEmail = principal != null ? principal.getEmail() : "admin@nqd.edu.vn";
        TeacherWithdrawalResponse response = adminFinanceService.rejectWithdrawal(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Tu choi yeu cau rut tien thanh cong", response));
    }

    @PostMapping("/withdrawals/{id}/complete")
    @Operation(summary = "Complete teacher withdrawal with bank transfer reference code")
    public ResponseEntity<ApiResponse<TeacherWithdrawalResponse>> completeWithdrawal(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminProcessWithdrawalRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String adminEmail = principal != null ? principal.getEmail() : "admin@nqd.edu.vn";
        TeacherWithdrawalResponse response = adminFinanceService.completeWithdrawal(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Xac nhan hoan tat chuyen tien thanh cong", response));
    }

    @PostMapping("/refunds")
    @Operation(summary = "Process refund for an order, reversing teacher earnings and revoking entitlements")
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @Valid @RequestBody ProcessRefundRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String adminEmail = principal != null ? principal.getEmail() : "admin@nqd.edu.vn";
        RefundResponse response = adminFinanceService.processRefund(request, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Xu ly hoan tien don hang thanh cong", response));
    }
}