package com.nqd.nqd_lms_be.billing.provider;

import java.util.Map;

public interface PaymentProvider {

    /**
     * Unique identifier name for this payment provider (e.g. PAYOS, VIETQR, SEPAY, VNPAY, MOMO)
     */
    String getProviderName();

    /**
     * Generates a payment intent, checkout URL, and dynamic QR Code for the customer to complete payment.
     */
    PaymentCreationResult createPayment(PaymentCreationCommand command);

    /**
     * Verifies the digital signature / checksum of an incoming webhook payload against this provider's secret key.
     */
    boolean verifyWebhookSignature(String rawPayload, String signature, Map<String, String> headers);

    /**
     * Parses the provider-specific webhook payload into a normalized `ParsedWebhookPayload`.
     */
    ParsedWebhookPayload parseWebhook(String rawPayload, Map<String, String> headers);

    /**
     * Optional refund processing for providers that support programmatic reversal.
     */
    default RefundResult refund(RefundCommand command) {
        return RefundResult.builder()
                .success(false)
                .message("Refund is not supported by " + getProviderName())
                .build();
    }
}
