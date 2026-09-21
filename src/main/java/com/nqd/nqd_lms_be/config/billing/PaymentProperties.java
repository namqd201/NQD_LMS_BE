package com.nqd.nqd_lms_be.config.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment")
@Getter
@Setter
public class PaymentProperties {

    /**
     * Primary payment provider name (e.g. PAYOS, VIETQR, SEPAY, VNPAY, MOMO)
     */
    private String provider = "PAYOS";

    /**
     * Client ID from the payment gateway provider
     */
    private String clientId = "nqd-lms-client-id";

    /**
     * API Key for communication with payment provider
     */
    private String apiKey = "nqd-lms-api-key";

    /**
     * Secret checksum key used to sign and verify HMAC-SHA256 signatures
     */
    private String checksumKey = "nqd-lms-checksum-secret-key-32chars";

    /**
     * Customer redirect URL upon successful checkout
     */
    private String returnUrl = "http://localhost:3000/payment/success";

    /**
     * Customer redirect URL if payment is cancelled
     */
    private String cancelUrl = "http://localhost:3000/payment/cancel";

    /**
     * Base webhook endpoint URL registered with the payment provider
     */
    private String webhookUrl = "http://localhost:8080/api/v1/payments/webhook";

    /**
     * Default currency for payment transactions
     */
    private String currency = "VND";

    /**
     * Expiration window in minutes for generated payment requests
     */
    private int expirationMinutes = 15;

    /**
     * VietQR beneficiary bank code (e.g. MB, VCB, TCB)
     */
    private String bankCode = "MB";

    /**
     * Beneficiary bank account number
     */
    private String accountNumber = "9999999999";

    /**
     * Beneficiary bank account holder name
     */
    private String accountName = "NQD LMS EDTECH";
}
