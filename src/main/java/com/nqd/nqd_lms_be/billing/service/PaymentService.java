package com.nqd.nqd_lms_be.billing.service;

import com.nqd.nqd_lms_be.billing.dto.*;
import com.nqd.nqd_lms_be.billing.provider.ParsedWebhookPayload;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiatePayment(UUID userId, CreatePaymentRequest request);

    ParsedWebhookPayload processWebhook(String providerName, String rawPayload, Map<String, String> headers);

    PaymentTransaction getPaymentById(UUID paymentId, UUID userId);

    PageResponse<PaymentTransactionResponse> getMyPayments(UUID userId, PaymentFilterRequest filter, Pageable pageable);

    PageResponse<PaymentTransactionResponse> getAdminPayments(PaymentFilterRequest filter, Pageable pageable);

    PaymentAnalyticsResponse getPaymentAnalytics();
}
