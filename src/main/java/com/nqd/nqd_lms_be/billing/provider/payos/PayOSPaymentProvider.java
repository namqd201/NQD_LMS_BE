package com.nqd.nqd_lms_be.billing.provider.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.billing.provider.*;
import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class PayOSPaymentProvider implements PaymentProvider {

    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    public PayOSPaymentProvider(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public String getProviderName() {
        return "PAYOS";
    }

    @Override
    public PaymentCreationResult createPayment(PaymentCreationCommand command) {
        try {
            String orderCode = command.getOrderCode();
            BigDecimal amount = command.getAmount();
            long amountLong = amount.longValue();

            // Calculate HMAC-SHA256 signature for PayOS request specification
            Map<String, Object> params = new TreeMap<>();
            params.put("amount", amountLong);
            params.put("cancelUrl", command.getCancelUrl() != null ? command.getCancelUrl() : paymentProperties.getCancelUrl());
            params.put("description", orderCode);
            params.put("orderCode", orderCode);
            params.put("returnUrl", command.getReturnUrl() != null ? command.getReturnUrl() : paymentProperties.getReturnUrl());

            String signData = createSortedQueryString(params);
            String signature = calculateHmacSha256(signData, paymentProperties.getChecksumKey());

            // Build standard dynamic VietQR QuickLink (EMVCo compliant image)
            String bankCode = paymentProperties.getBankCode();
            String accountNumber = paymentProperties.getAccountNumber();
            String accountName = URLEncoder.encode(paymentProperties.getAccountName(), StandardCharsets.UTF_8);
            String memo = URLEncoder.encode(orderCode, StandardCharsets.UTF_8);

            String qrCodeUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    bankCode, accountNumber, amountLong, memo, accountName
            );

            // Checkout page URL
            String checkoutUrl = String.format(
                    "%s?orderCode=%s&amount=%d&signature=%s",
                    paymentProperties.getReturnUrl().replace("/payment/success", "/payment/checkout"),
                    orderCode, amountLong, signature
            );

            LocalDateTime expiresAt = command.getExpiresAt() != null
                    ? command.getExpiresAt()
                    : LocalDateTime.now().plusMinutes(paymentProperties.getExpirationMinutes());

            String providerTxId = "PAYOS_" + orderCode + "_" + System.currentTimeMillis();

            return PaymentCreationResult.builder()
                    .success(true)
                    .provider(getProviderName())
                    .providerTransactionId(providerTxId)
                    .orderCode(orderCode)
                    .amount(amount)
                    .currency(command.getCurrency() != null ? command.getCurrency() : paymentProperties.getCurrency())
                    .qrCode(qrCodeUrl)
                    .checkoutUrl(checkoutUrl)
                    .signature(signature)
                    .expiresAt(expiresAt)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate payment intent with PayOS provider for order {}: ", command.getOrderCode(), e);
            return PaymentCreationResult.builder()
                    .success(false)
                    .provider(getProviderName())
                    .orderCode(command.getOrderCode())
                    .errorMessage("Không thể khởi tạo cổng thanh toán PayOS: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signature, Map<String, String> headers) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return false;
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            // 1. Signature passed in header 'x-signature' or 'x-payos-signature' or JSON body
            String sig = signature;
            if (sig == null || sig.isBlank()) {
                if (headers != null) {
                    sig = headers.getOrDefault("x-signature",
                            headers.getOrDefault("x-payos-signature",
                            headers.getOrDefault("x-hub-signature", null)));
                }
                if (sig == null && root.has("signature")) {
                    sig = root.get("signature").asText();
                }
            }

            if (sig == null || sig.isBlank()) {
                log.warn("PayOS webhook verification failed: No signature provided in body or headers");
                return false;
            }

            // 2. Extract Data node if payload wraps data in `data` object (PayOS format)
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            // 3. Sort keys and compute HMAC-SHA256
            Map<String, Object> sortedMap = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if ("signature".equals(key)) {
                    continue; // Skip signature field itself
                }
                JsonNode value = field.getValue();
                if (value.isValueNode()) {
                    if (value.isNumber()) {
                        sortedMap.put(key, value.numberValue());
                    } else if (value.isBoolean()) {
                        sortedMap.put(key, value.booleanValue());
                    } else {
                        sortedMap.put(key, value.asText());
                    }
                }
            }

            String signData = createSortedQueryString(sortedMap);
            String expectedSignature = calculateHmacSha256(signData, paymentProperties.getChecksumKey());

            // Constant time comparison to prevent timing attacks
            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    sig.getBytes(StandardCharsets.UTF_8)
            );

            if (!isValid) {
                log.warn("PayOS signature mismatch! Computed: {}, Received: {}", expectedSignature, sig);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying PayOS webhook signature: ", e);
            return false;
        }
    }

    @Override
    public ParsedWebhookPayload parseWebhook(String rawPayload, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            // Extract orderCode
            String orderCode = null;
            if (dataNode.has("orderCode")) {
                orderCode = dataNode.get("orderCode").asText();
            } else if (dataNode.has("description")) {
                orderCode = extractOrderCodeFromText(dataNode.get("description").asText());
            } else if (dataNode.has("content")) {
                orderCode = extractOrderCodeFromText(dataNode.get("content").asText());
            } else if (dataNode.has("code")) {
                orderCode = dataNode.get("code").asText();
            }

            // Extract Amount
            BigDecimal amount = BigDecimal.ZERO;
            if (dataNode.has("amount")) {
                amount = new BigDecimal(dataNode.get("amount").asText());
            } else if (dataNode.has("transferAmount")) {
                amount = new BigDecimal(dataNode.get("transferAmount").asText());
            }

            // Extract Status
            PaymentStatus status = PaymentStatus.SUCCESS;
            if (root.has("code")) {
                String code = root.get("code").asText();
                if (!"00".equals(code) && !"0".equals(code) && !"SUCCESS".equalsIgnoreCase(code)) {
                    status = PaymentStatus.FAILED;
                }
            }

            // Extract Provider Transaction ID & References
            String providerTxId = dataNode.has("paymentLinkId")
                    ? dataNode.get("paymentLinkId").asText()
                    : (dataNode.has("id") ? dataNode.get("id").asText() : "TXN_" + System.currentTimeMillis());

            String gatewayRef = dataNode.has("reference")
                    ? dataNode.get("reference").asText()
                    : (dataNode.has("referenceCode") ? dataNode.get("referenceCode").asText() : null);

            LocalDateTime txDateTime = LocalDateTime.now();
            if (dataNode.has("transactionDateTime")) {
                try {
                    String dtStr = dataNode.get("transactionDateTime").asText();
                    txDateTime = LocalDateTime.parse(dtStr.replace("Z", ""), DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception ex) {
                    txDateTime = LocalDateTime.now();
                }
            }

            boolean isSignatureValid = verifyWebhookSignature(rawPayload, null, headers);

            return ParsedWebhookPayload.builder()
                    .provider(getProviderName())
                    .orderCode(orderCode)
                    .providerTransactionId(providerTxId)
                    .amount(amount)
                    .currency("VND")
                    .status(status)
                    .transactionDateTime(txDateTime)
                    .gatewayReference(gatewayRef)
                    .rawReference(dataNode.toString())
                    .rawPayload(rawPayload)
                    .isSignatureValid(isSignatureValid)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse PayOS webhook payload: ", e);
            return ParsedWebhookPayload.builder()
                    .provider(getProviderName())
                    .isSignatureValid(false)
                    .errorMessage("Không thể phân tích dữ liệu webhook: " + e.getMessage())
                    .rawPayload(rawPayload)
                    .build();
        }
    }

    private String createSortedQueryString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue().toString());
            }
        }
        return sb.toString();
    }

    public static String calculateHmacSha256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    private String extractOrderCodeFromText(String text) {
        if (text == null) return null;
        // Match NQD_COURSE_... or ORD-... or any alphanumeric code
        for (String part : text.split("[\\s,;:]+")) {
            if (part.startsWith("NQD_") || part.startsWith("ORD-") || part.startsWith("ORD_")) {
                return part.trim();
            }
        }
        return text.trim();
    }
}
