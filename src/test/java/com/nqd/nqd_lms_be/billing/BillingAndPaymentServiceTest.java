package com.nqd.nqd_lms_be.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.billing.dto.CreatePaymentRequest;
import com.nqd.nqd_lms_be.billing.dto.PaymentAnalyticsResponse;
import com.nqd.nqd_lms_be.billing.dto.PaymentResponse;
import com.nqd.nqd_lms_be.billing.notification.BillingNotificationService;
import com.nqd.nqd_lms_be.billing.provider.ParsedWebhookPayload;
import com.nqd.nqd_lms_be.billing.provider.payos.PayOSPaymentProvider;
import com.nqd.nqd_lms_be.billing.service.EntitlementActivationService;
import com.nqd.nqd_lms_be.billing.service.OrderService;
import com.nqd.nqd_lms_be.billing.service.PaymentService;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.config.billing.PaymentProperties;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class BillingAndPaymentServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentProperties paymentProperties;

    @MockitoBean
    private BillingNotificationService billingNotificationService;

    private User student;
    private Course course;
    private MembershipPlan plan;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        student = userRepository.save(User.builder()
                .email("student_billing_" + System.currentTimeMillis() + "@test.com")
                .fullName("Billing Student")
                .status(UserStatus.ACTIVE)
                .build());

        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(Subject.builder().name("Toán Học").code("MATH_" + System.currentTimeMillis()).build())
        );

        course = courseRepository.save(Course.builder()
                .name("Toán 12 Cơ Bản")
                .code("MATH12_" + System.currentTimeMillis())
                .subject(subject)
                .status(CourseStatus.ACTIVE)
                .build());

        plan = membershipPlanRepository.save(MembershipPlan.builder()
                .planCode("VIP_STUDENT_" + System.currentTimeMillis())
                .name("Gói Học Sinh VIP")
                .price(new BigDecimal("199000.00"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTHLY)
                .status(ProductStatus.PUBLISHED)
                .build());
    }

    private String buildPayOSSignedWebhook(String orderCode, BigDecimal amount) {
        long amountLong = amount.longValue();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("accountNumber", paymentProperties.getAccountNumber());
        dataMap.put("amount", amountLong);
        dataMap.put("code", "00");
        dataMap.put("currency", "VND");
        dataMap.put("description", orderCode);
        dataMap.put("orderCode", orderCode);
        dataMap.put("paymentLinkId", "PLINK_" + System.currentTimeMillis());
        dataMap.put("reference", "REF_" + System.currentTimeMillis());
        dataMap.put("transactionDateTime", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String signature = PayOSPaymentProvider.calculateHmacSha256(sb.toString(), paymentProperties.getChecksumKey());

        Map<String, Object> root = new HashMap<>();
        root.put("code", "00");
        root.put("desc", "success");
        root.put("data", dataMap);
        root.put("signature", signature);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Create course order and initiate QR payment successfully")
    void testCreateCourseOrderAndInitiatePayment() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_CRS_01");

        assertNotNull(order);
        assertNotNull(order.getOrderCode());
        assertTrue(order.getOrderCode().startsWith("NQDCOURSE"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(new BigDecimal("499000.00"), order.getFinalAmount());

        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.VIETQR)
                .build();

        PaymentResponse paymentResponse = paymentService.initiatePayment(student.getId(), paymentRequest);

        assertNotNull(paymentResponse);
        assertNotNull(paymentResponse.getPaymentId());
        assertNotNull(paymentResponse.getQrCode());
        assertTrue(paymentResponse.getQrCode().contains("vietqr.io"));
        assertEquals(order.getOrderCode(), paymentResponse.getOrderCode());
        assertEquals(PaymentStatus.PENDING, paymentResponse.getStatus());
    }

    @Test
    @DisplayName("2. Process valid webhook: marks Order PAID, activates Course Enrollment exactly once")
    void testSuccessfulPaymentWebhookAndActivation() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_CRS_02");
        String webhookPayload = buildPayOSSignedWebhook(order.getOrderCode(), order.getFinalAmount());

        ParsedWebhookPayload result = paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        assertNotNull(result);
        assertEquals(order.getOrderCode(), result.getOrderCode());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        assertNotNull(updatedOrder.getCompletedAt());

        // Verify Course Enrollment was activated
        Optional<CourseEnrollment> enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(course.getId(), student.getId());
        assertTrue(enrollment.isPresent());
        assertEquals(EnrollmentStatus.ENROLLED, enrollment.get().getStatus());

        // Verify Entitlement record created
        Optional<Entitlement> entitlement = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                student.getId(), EntitlementType.COURSE_ACCESS, course.getId(), EntitlementStatus.ACTIVE
        );
        assertTrue(entitlement.isPresent());

        // Verify notification sent
        verify(billingNotificationService, times(1)).sendPaymentSuccessNotification(any());
    }

    @Test
    @DisplayName("3. Process webhook with invalid signature throws SecurityException")
    void testInvalidSignatureThrowsSecurityException() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_CRS_03");

        String roguePayload = "{\"code\":\"00\",\"data\":{\"orderCode\":\"" + order.getOrderCode() + "\",\"amount\":499000},\"signature\":\"invalid_signature_12345\"}";

        assertThrows(SecurityException.class, () ->
                paymentService.processWebhook("PAYOS", roguePayload, Map.of())
        );

        Order unalteredOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, unalteredOrder.getStatus());
    }

    @Test
    @DisplayName("4. Process webhook with wrong amount throws IllegalArgumentException and prevents activation")
    void testWrongAmountThrowsIllegalArgumentException() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_CRS_04");

        // Webhook sends 100,000 instead of 499,000
        String underpaidPayload = buildPayOSSignedWebhook(order.getOrderCode(), new BigDecimal("100000.00"));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processWebhook("PAYOS", underpaidPayload, Map.of())
        );

        Order unalteredOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, unalteredOrder.getStatus());
        assertTrue(courseEnrollmentRepository.findByCourseIdAndStudentId(course.getId(), student.getId()).isEmpty());
    }

    @Test
    @DisplayName("5. Process webhook for unknown order throws ResourceNotFoundException")
    void testUnknownOrderThrowsResourceNotFoundException() {
        String unknownPayload = buildPayOSSignedWebhook("NQD_COURSE_UNKNOWN_999999", new BigDecimal("500000.00"));

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processWebhook("PAYOS", unknownPayload, Map.of())
        );
    }

    @Test
    @DisplayName("6. Duplicate webhook calls are idempotent: activations happen exactly once")
    void testDuplicateWebhookIdempotency() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_CRS_06");
        String webhookPayload = buildPayOSSignedWebhook(order.getOrderCode(), order.getFinalAmount());

        // First delivery
        paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        // Second delivery (simulating gateway retry)
        paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        // Third delivery
        paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        Order finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, finalOrder.getStatus());

        // Verify only 1 CourseEnrollment exists
        long enrollmentCount = courseEnrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()) && e.getStudent().getId().equals(student.getId()))
                .count();
        assertEquals(1, enrollmentCount);

        // Verify notification called only once
        verify(billingNotificationService, times(1)).sendPaymentSuccessNotification(any());
    }

    @Test
    @DisplayName("7. Membership order payment activates Subscription and Entitlement")
    void testMembershipOrderActivation() {
        Order order = orderService.createMembershipOrder(student.getId(), plan.getId(), null, "IDEMP_MEM_01");
        String webhookPayload = buildPayOSSignedWebhook(order.getOrderCode(), order.getFinalAmount());

        paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());

        List<Subscription> subs = subscriptionRepository.findByUserIdAndStatusAndIsDeletedFalse(student.getId(), SubscriptionStatus.ACTIVE);
        assertFalse(subs.isEmpty());
        assertEquals(plan.getId(), subs.get(0).getMembershipPlan().getId());
        assertTrue(subs.get(0).isCurrentlyActive());
    }

    @Test
    @DisplayName("8. Payment analytics aggregates revenue and order counts accurately")
    void testPaymentAnalytics() {
        Order order = orderService.createCourseOrder(student.getId(), course.getId(), null, "IDEMP_ANALYTICS");
        String webhookPayload = buildPayOSSignedWebhook(order.getOrderCode(), order.getFinalAmount());
        paymentService.processWebhook("PAYOS", webhookPayload, Map.of());

        PaymentAnalyticsResponse analytics = paymentService.getPaymentAnalytics();

        assertNotNull(analytics);
        assertTrue(analytics.getPaidOrdersCount() >= 1);
        assertTrue(analytics.getRevenueToday().compareTo(BigDecimal.ZERO) > 0);
    }
}
