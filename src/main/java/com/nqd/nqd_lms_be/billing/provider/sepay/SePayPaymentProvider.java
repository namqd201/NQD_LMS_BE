package com.nqd.nqd_lms_be.billing.provider.sepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.billing.provider.*;
import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SePay Automated Bank Transfer & VietQR Payment Provider.
 * Integrates directly with SePay.vn Webhook & dynamic VietQR generation.
 */
@Component
@Slf4j
public class SePayPaymentProvider implements PaymentProvider {

    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(NQD[A-Za-z0-9]+|ORD[A-Za-z0-9]+|MEM[A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

    public SePayPaymentProvider(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public String getProviderName() {
        return "SEPAY";
    }

    @Override
    public PaymentCreationResult createPayment(PaymentCreationCommand command) {
        try {
            String orderCode = command.getOrderCode();
            BigDecimal amount = command.getAmount();
            long amountLong = amount != null ? amount.longValue() : 0L;

            String bankCode = paymentProperties.getBankCode() != null ? paymentProperties.getBankCode() : "MB";
            String accountNumber = paymentProperties.getAccountNumber() != null ? paymentProperties.getAccountNumber() : "9999999999";
            String accountName = paymentProperties.getAccountName() != null ? paymentProperties.getAccountName() : "NQD LMS EDTECH";
            String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
            String encodedDes = URLEncoder.encode(orderCode, StandardCharsets.UTF_8);

            // 1. Dynamic SePay QR Link (template=compact2)
            // https://qr.sepay.vn/img?acc={acc}&bank={bank}&amount={amount}&des={des}&template=compact2
            String sepayQrUrl = String.format(
                    "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=compact2",
                    accountNumber, bankCode, amountLong, orderCode
            );

            // 2. Standard VietQR fallback
            String vietQrUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    bankCode, accountNumber, amountLong, encodedDes, encodedName
            );

            // 3. SePay Gateway Hosted Checkout (optional)
            String checkoutUrl = String.format(
                    "https://qr.sepay.vn/gateway?acc=%s&bank=%s&amount=%d&des=%s",
                    accountNumber, bankCode, amountLong, encodedDes
            );

            return PaymentCreationResult.builder()
                    .success(true)
                    .providerTransactionId("SEPAY_" + orderCode + "_" + System.currentTimeMillis())
                    .orderCode(orderCode)
                    .qrCode(sepayQrUrl)
                    .checkoutUrl(checkoutUrl)
                    .expiresAt(command.getExpiresAt() != null ? command.getExpiresAt() : LocalDateTime.now().plusMinutes(paymentProperties.getExpirationMinutes()))
                    .build();
        } catch (Exception e) {
            log.error("Error creating SePay payment for order {}", command.getOrderCode(), e);
            return PaymentCreationResult.builder()
                    .success(false)
                    .errorMessage("Lỗi tạo mã thanh toán SePay: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signature, Map<String, String> headers) {
        String expectedApiKey = paymentProperties.getApiKey();

        // If in local/dev environment or default placeholder key is used, allow verification
        if (expectedApiKey == null || expectedApiKey.isBlank() || "nqd-lms-api-key".equals(expectedApiKey)) {
            log.debug("SePay API Key is not set or default. Permitting webhook for local/sandbox testing.");
            return true;
        }

        // Case-insensitive header check for Authorization, Apikey, x-api-key
        String authHeader = null;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if ("authorization".equals(key) || "x-api-key".equals(key) || "apikey".equals(key)) {
                    authHeader = entry.getValue();
                    break;
                }
            }
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("SePay Webhook missing authorization header");
            return false;
        }

        // Format could be 'Apikey <token>', 'Bearer <token>', or just '<token>'
        String token = authHeader.replaceAll("(?i)^(Apikey|Bearer)\\s+", "").trim();
        boolean matches = expectedApiKey.trim().equals(token);
        if (!matches) {
            log.warn("SePay Webhook API Key mismatch. Expected token configured in system.");
        }
        return matches;
    }

    @Override
    public ParsedWebhookPayload parseWebhook(String rawPayload, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            // Check if SePay payload or nested data payload
            JsonNode dataNode = root.has("data") ? root.path("data") : root;

            String transferType = dataNode.path("transferType").asText("in");
            if (!"in".equalsIgnoreCase(transferType)) {
                log.warn("Ignoring non-inflow SePay transaction with transferType: {}", transferType);
                return null;
            }

            BigDecimal amount = BigDecimal.valueOf(dataNode.path("transferAmount").asDouble(0.0));
            String content = dataNode.path("content").asText("");
            String description = dataNode.path("description").asText("");
            String code = dataNode.path("code").asText("");
            String refCode = dataNode.path("referenceCode").asText(dataNode.path("id").asText(""));
            String gateway = dataNode.path("gateway").asText("SEPAY");

            // Extract orderCode
            String orderCode = extractOrderCode(code, content, description);

            LocalDateTime transactionTime = LocalDateTime.now();
            if (dataNode.hasNonNull("transactionDate")) {
                try {
                    String dtStr = dataNode.path("transactionDate").asText();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    transactionTime = LocalDateTime.parse(dtStr, formatter);
                } catch (Exception ignored) {}
            }

            return ParsedWebhookPayload.builder()
                    .orderCode(orderCode)
                    .providerTransactionId(refCode)
                    .amount(amount)
                    .currency("VND")
                    .status(PaymentStatus.PAID)
                    .transactionDateTime(transactionTime)
                    .gatewayReference(gateway + " - " + refCode)
                    .rawReference(rawPayload)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse SePay webhook payload: {}", rawPayload, e);
            throw new RuntimeException("Lỗi phân tích dữ liệu webhook SePay: " + e.getMessage(), e);
        }
    }

    private String extractOrderCode(String code, String content, String description) {
        if (code != null && !code.isBlank()) {
            String upper = code.trim().toUpperCase();
            if (upper.startsWith("NQD") || upper.startsWith("ORD") || upper.startsWith("MEM")) {
                return upper;
            }
        }

        String fullText = (content + " " + description).trim();
        Matcher matcher = ORDER_CODE_PATTERN.matcher(fullText);
        if (matcher.find()) {
            return matcher.group(1).trim().toUpperCase();
        }

        if (!content.isBlank()) {
            String[] tokens = content.split("[\\s,;.-]+");
            for (String t : tokens) {
                String upper = t.trim().toUpperCase();
                if (upper.startsWith("NQD") || upper.startsWith("ORD") || upper.startsWith("MEM")) {
                    return upper;
                }
            }
        }

        return content.trim();
    }
}
