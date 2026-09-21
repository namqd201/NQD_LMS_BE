package com.nqd.nqd_lms_be.controller.billing;

import com.nqd.nqd_lms_be.billing.dto.*;
import com.nqd.nqd_lms_be.billing.provider.ParsedWebhookPayload;
import com.nqd.nqd_lms_be.billing.service.PaymentService;
import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing - Payments", description = "Endpoints for initiating QR payments, processing webhooks, and checking history")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProperties paymentProperties;

    @GetMapping("/config")
    @Operation(summary = "Get public bank and payment configuration for Checkout UI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicPaymentConfig() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy cấu hình thanh toán thành công", Map.of(
                "provider", paymentProperties.getProvider(),
                "bankCode", paymentProperties.getBankCode(),
                "accountNumber", paymentProperties.getAccountNumber(),
                "accountName", paymentProperties.getAccountName(),
                "currency", paymentProperties.getCurrency()
        )));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Generate dynamic QR Code and payment intent for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        PaymentResponse response = paymentService.initiatePayment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Khởi tạo mã thanh toán QR thành công", response));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Default Payment Gateway Webhook receiver (PayOS/VietQR/SePay)")
    public ResponseEntity<Map<String, Object>> handleDefaultWebhook(
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers
    ) {
        String provider = paymentProperties.getProvider() != null ? paymentProperties.getProvider() : "SEPAY";
        log.info("Received default payment gateway webhook for provider: {}", provider);
        ParsedWebhookPayload result = paymentService.processWebhook(provider, rawPayload, headers);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook processed successfully",
                "orderCode", result.getOrderCode() != null ? result.getOrderCode() : "",
                "status", result.getStatus() != null ? result.getStatus().name() : "SUCCESS"
        ));
    }

    @PostMapping("/webhook/{provider}")
    @Operation(summary = "Provider-specific Webhook receiver")
    public ResponseEntity<Map<String, Object>> handleProviderWebhook(
            @PathVariable String provider,
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("Received payment gateway webhook for provider: {}", provider);
        ParsedWebhookPayload result = paymentService.processWebhook(provider, rawPayload, headers);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook processed successfully for " + provider,
                "orderCode", result.getOrderCode() != null ? result.getOrderCode() : "",
                "status", result.getStatus() != null ? result.getStatus().name() : "SUCCESS"
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get current authenticated user's payment transaction history")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getMyPayments(
            @ModelAttribute PaymentFilterRequest filter,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = paymentService.getMyPayments(principal.getId(), filter, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử giao dịch thành công", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get payment transaction details by ID")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getPaymentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        PaymentTransaction txn = paymentService.getPaymentById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết giao dịch thành công", PaymentTransactionResponse.fromEntity(txn)));
    }
}
