package com.nqd.nqd_lms_be.billing.service;

import com.nqd.nqd_lms_be.billing.dto.*;
import com.nqd.nqd_lms_be.billing.provider.*;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import com.nqd.nqd_lms_be.entity.Order;
import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import com.nqd.nqd_lms_be.entity.enums.PaymentMethod;
import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import com.nqd.nqd_lms_be.repository.OrderRepository;
import com.nqd.nqd_lms_be.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final EntitlementActivationService entitlementActivationService;
    private final PaymentProperties paymentProperties;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID userId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn không có quyền thực hiện thanh toán cho đơn hàng này.");
        }

        if (order.getStatus().isPaid()) {
            throw new IllegalStateException("Đơn hàng này đã được thanh toán hoàn tất trước đó.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new IllegalStateException("Đơn hàng đã bị hủy hoặc hết hạn, không thể tiếp tục thanh toán.");
        }

        String providerName = request.getProvider() != null && !request.getProvider().isBlank()
                ? request.getProvider()
                : paymentProperties.getProvider();

        PaymentProvider provider = paymentProviderFactory.getProvider(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Không tìm thấy cổng thanh toán: " + providerName);
        }

        // Generate or reuse transaction
        String txCode = generateTransactionCode(order.getOrderCode());
        String idempotencyKey = "PAY_TXN_" + order.getOrderCode() + "_" + provider.getProviderName();

        PaymentCreationCommand command = PaymentCreationCommand.builder()
                .orderCode(order.getOrderCode())
                .amount(order.getFinalAmount())
                .currency(order.getCurrency())
                .description("Thanh toán " + order.getOrderCode())
                .buyerEmail(order.getUser().getEmail())
                .buyerName(order.getUser().getFullName())
                .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : paymentProperties.getReturnUrl())
                .cancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : paymentProperties.getCancelUrl())
                .expiresAt(LocalDateTime.now().plusMinutes(paymentProperties.getExpirationMinutes()))
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentCreationResult result = provider.createPayment(command);
        if (!result.isSuccess()) {
            throw new RuntimeException("Lỗi khởi tạo thanh toán từ cổng " + provider.getProviderName() + ": " + result.getErrorMessage());
        }

        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.VIETQR;

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(txCode)
                .order(order)
                .provider(provider.getProviderName())
                .providerTransactionId(result.getProviderTransactionId())
                .paymentMethod(method)
                .amount(order.getFinalAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .gatewayReference(result.getCheckoutUrl())
                .rawReference(result.getQrCode())
                .build();

        PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);

        return PaymentResponse.builder()
                .paymentId(savedTxn.getId())
                .transactionCode(savedTxn.getTransactionCode())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(savedTxn.getAmount())
                .currency(savedTxn.getCurrency())
                .provider(provider.getProviderName())
                .status(savedTxn.getStatus())
                .qrCode(result.getQrCode())
                .checkoutUrl(result.getCheckoutUrl())
                .signature(result.getSignature())
                .expiresAt(result.getExpiresAt())
                .createdAt(savedTxn.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ParsedWebhookPayload processWebhook(String providerName, String rawPayload, Map<String, String> headers) {
        log.info("Received incoming webhook for provider: {}", providerName);

        PaymentProvider provider = paymentProviderFactory.getProvider(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Không xác định được cổng thanh toán từ webhook: " + providerName);
        }

        // 1. Validate signature
        boolean isValidSignature = provider.verifyWebhookSignature(rawPayload, null, headers);
        if (!isValidSignature) {
            log.error("CRITICAL SECURITY: Invalid webhook signature detected for provider {}! Payload: {}", providerName, rawPayload);
            throw new SecurityException("Chữ ký số webhook không hợp lệ (Invalid webhook signature).");
        }

        // 2. Parse payload
        ParsedWebhookPayload payload = provider.parseWebhook(rawPayload, headers);
        if (payload == null || payload.getOrderCode() == null || payload.getOrderCode().isBlank()) {
            log.warn("Webhook payload does not contain a valid orderCode: {}", rawPayload);
            throw new IllegalArgumentException("Không tìm thấy mã đơn hàng (orderCode) trong dữ liệu webhook.");
        }

        String orderCode = payload.getOrderCode();
        log.info("Processing webhook payment confirmation for order #{}", orderCode);

        // 3. Locate Order with pessimistic row lock to prevent race conditions on concurrent webhooks
        Order order = orderRepository.findByOrderCodeWithLock(orderCode)
                .or(() -> {
                    if (!orderCode.startsWith("NQD")) {
                        return orderRepository.findByOrderCodeWithLock("NQD" + orderCode);
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        // 4. Verify Amount
        if (payload.getAmount() == null || payload.getAmount().compareTo(order.getFinalAmount()) < 0) {
            log.error("CRITICAL AMOUNT MISMATCH: Order #{} expects {}, but webhook received {}!",
                    orderCode, order.getFinalAmount(), payload.getAmount());
            throw new IllegalArgumentException(String.format(
                    "Số tiền thanh toán (%s) không khớp với số tiền đơn hàng (%s).",
                    payload.getAmount(), order.getFinalAmount()
            ));
        }

        // 5. Idempotency Check: If order is ALREADY PAID/COMPLETED -> Return early
        if (order.getStatus().isPaid()) {
            log.info("IDEMPOTENCY: Order #{} is already marked as PAID/COMPLETED. Skipping duplicate fulfillment.", orderCode);
            return payload;
        }

        // 6. Update or Create PaymentTransaction
        PaymentTransaction txn = paymentTransactionRepository.findByOrderIdAndIsDeletedFalseOrderByCreatedAtDesc(order.getId())
                .stream().findFirst()
                .orElseGet(() -> PaymentTransaction.builder()
                        .transactionCode("TXN_WH_" + orderCode + "_" + System.currentTimeMillis())
                        .order(order)
                        .provider(provider.getProviderName())
                        .paymentMethod(PaymentMethod.VIETQR)
                        .amount(payload.getAmount())
                        .currency(order.getCurrency())
                        .build());

        txn.setStatus(PaymentStatus.PAID);
        txn.setPaidAt(payload.getTransactionDateTime() != null ? payload.getTransactionDateTime() : LocalDateTime.now());
        txn.setProcessedAt(LocalDateTime.now());
        txn.setProviderTransactionId(payload.getProviderTransactionId());
        txn.setGatewayReference(payload.getGatewayReference());
        txn.setRawCallbackPayload(rawPayload);
        txn.setRawReference(payload.getRawReference());

        paymentTransactionRepository.save(txn);

        // 7. Mark Order as PAID
        order.setStatus(OrderStatus.PAID);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 8. Auto-Activate Entitlements & Dispatch Notifications
        entitlementActivationService.activateOrderFulfillment(order);

        log.info("Webhook processing and fulfillment successfully completed for order #{}", orderCode);
        return payload;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getPaymentById(UUID paymentId, UUID userId) {
        PaymentTransaction txn = paymentTransactionRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với ID: " + paymentId));

        if (userId != null && txn.getOrder() != null && !txn.getOrder().getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn không có quyền xem thông tin giao dịch của người dùng khác.");
        }

        return txn;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getMyPayments(UUID userId, PaymentFilterRequest filter, Pageable pageable) {
        Specification<PaymentTransaction> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("isDeleted"), false));
            predicates.add(cb.equal(root.get("order").get("user").get("id"), userId));

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getProvider() != null && !filter.getProvider().isBlank()) {
                    predicates.add(cb.equal(root.get("provider"), filter.getProvider().trim().toUpperCase()));
                }
                if (filter.getOrderCode() != null && !filter.getOrderCode().isBlank()) {
                    predicates.add(cb.like(cb.upper(root.get("order").get("orderCode")), "%" + filter.getOrderCode().trim().toUpperCase() + "%"));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PaymentTransaction> page = paymentTransactionRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page, PaymentTransactionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getAdminPayments(PaymentFilterRequest filter, Pageable pageable) {
        Specification<PaymentTransaction> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getProvider() != null && !filter.getProvider().isBlank()) {
                    predicates.add(cb.equal(root.get("provider"), filter.getProvider().trim().toUpperCase()));
                }
                if (filter.getOrderCode() != null && !filter.getOrderCode().isBlank()) {
                    predicates.add(cb.like(cb.upper(root.get("order").get("orderCode")), "%" + filter.getOrderCode().trim().toUpperCase() + "%"));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PaymentTransaction> page = paymentTransactionRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page, PaymentTransactionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAnalyticsResponse getPaymentAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        LocalDateTime startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        BigDecimal revenueToday = paymentTransactionRepository.sumPaymentAmountBetween(startOfToday, endOfToday);
        BigDecimal revenueThisMonth = paymentTransactionRepository.sumPaymentAmountBetween(startOfMonth, endOfMonth);
        BigDecimal totalRevenue = paymentTransactionRepository.sumPaymentAmountBetween(
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.now().plusDays(1)
        );

        long paidOrders = orderRepository.countPaidOrders();
        long pendingOrders = orderRepository.countPendingOrders();
        long failedPayments = paymentTransactionRepository.countFailedPayments();
        long successfulPayments = paymentTransactionRepository.countSuccessfulPayments();

        return PaymentAnalyticsResponse.builder()
                .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .paidOrdersCount(paidOrders)
                .pendingOrdersCount(pendingOrders)
                .failedPaymentsCount(failedPayments)
                .successfulPaymentsCount(successfulPayments)
                .build();
    }

    private String generateTransactionCode(String orderCode) {
        String randomSuffix = String.format("%04d", RANDOM.nextInt(10000));
        return "TXN_" + orderCode.replace("NQD", "") + "_" + randomSuffix;
    }
}
