package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.billing.dto.PaymentAnalyticsResponse;
import com.nqd.nqd_lms_be.billing.dto.PaymentFilterRequest;
import com.nqd.nqd_lms_be.billing.dto.PaymentTransactionResponse;
import com.nqd.nqd_lms_be.billing.service.PaymentService;
import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Payments", description = "Administrative endpoints for payment audit, analytics and transaction inspection")
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all payment transactions with filters and pagination (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getAllPayments(
            @ModelAttribute PaymentFilterRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = paymentService.getAdminPayments(filter, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách giao dịch thành công", response));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get billing revenue analytics (revenue today, this month, paid/pending/failed counts)")
    public ResponseEntity<ApiResponse<PaymentAnalyticsResponse>> getPaymentAnalytics() {
        PaymentAnalyticsResponse analytics = paymentService.getPaymentAnalytics();
        return ResponseEntity.ok(ApiResponse.ok("Lấy thống kê doanh thu thành công", analytics));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment transaction details by ID (Admin only)")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getPaymentById(@PathVariable UUID id) {
        PaymentTransaction txn = paymentService.getPaymentById(id, null);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết giao dịch thành công", PaymentTransactionResponse.fromEntity(txn)));
    }
}
