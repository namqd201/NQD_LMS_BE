package com.nqd.nqd_lms_be.finance;

import com.nqd.nqd_lms_be.billing.service.EntitlementActivationService;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.finance.dto.*;
import com.nqd.nqd_lms_be.finance.service.AdminFinanceService;
import com.nqd.nqd_lms_be.finance.service.TeacherFinanceService;
import com.nqd.nqd_lms_be.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TeacherFinanceAndRevenueShareTest {

    @Autowired
    private TeacherFinanceService teacherFinanceService;

    @Autowired
    private AdminFinanceService adminFinanceService;

    @Autowired
    private EntitlementActivationService entitlementActivationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TeacherEarningRepository teacherEarningRepository;

    @Autowired
    private TeacherBankAccountRepository teacherBankAccountRepository;

    @Autowired
    private TeacherWithdrawalRepository teacherWithdrawalRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    private User teacher;
    private User student;
    private Subject subject;
    private Course paidCourse;
    private Product courseProduct;

    @BeforeEach
    void setUp() {
        teacher = userRepository.save(User.builder()
                .email("teacher.finance." + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Teacher Finance Master")
                .status(UserStatus.ACTIVE)
                .build());

        student = userRepository.save(User.builder()
                .email("student.finance." + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Student Learner")
                .status(UserStatus.ACTIVE)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Toan Nang Cao " + UUID.randomUUID())
                .code("MATH_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .description("Khoa hoc toan cao cap")
                .build());

        paidCourse = courseRepository.save(Course.builder()
                .subject(subject)
                .name("Toan 12 On Thi THPT Quoc Gia")
                .code("COURSE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(CourseStatus.PUBLISHED)
                .pricingType(CoursePricingType.PAID)
                .price(new BigDecimal("299000.00"))
                .currency("VND")
                .creator(teacher)
                .build());

        courseProduct = productRepository.save(Product.builder()
                .productType(ProductType.COURSE)
                .targetEntityId(paidCourse.getId())
                .title(paidCourse.getName())
                .code("PROD_" + paidCourse.getCode())
                .basePrice(paidCourse.getPrice())
                .currency("VND")
                .status(ProductStatus.PUBLISHED)
                .build());
    }

    private Order createPaidOrder(BigDecimal price) {
        Order order = Order.builder()
                .orderCode("ORD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6))
                .user(student)
                .totalAmount(price)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(price)
                .currency("VND")
                .status(OrderStatus.PAID)
                .placedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(courseProduct)
                .productTitle(courseProduct.getTitle())
                .productType(ProductType.COURSE)
                .unitPrice(price)
                .quantity(1)
                .subtotal(price)
                .currency("VND")
                .build();

        order.setItems(List.of(item));
        return orderRepository.save(order);
    }

    @Test
    @DisplayName("1. Revenue Split Calculation: 299,000 VND @ 20% platform fee = 59,800 fee, 239,200 teacher")
    void testRevenueSplitCalculationAndSnapshot() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        List<TeacherEarning> earnings = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId());
        assertThat(earnings).hasSize(1);

        TeacherEarning earning = earnings.get(0);
        assertThat(earning.getTeacher().getId()).isEqualTo(teacher.getId());
        assertThat(earning.getGrossAmount()).isEqualByComparingTo("299000.00");
        assertThat(earning.getPlatformFee()).isEqualByComparingTo("59800.00");
        assertThat(earning.getTeacherAmount()).isEqualByComparingTo("239200.00");
        assertThat(earning.getPlatformFeeRate()).isEqualByComparingTo("0.2000");
        assertThat(earning.getTeacherShareRate()).isEqualByComparingTo("0.8000");
        assertThat(earning.getCurrency()).isEqualTo("VND");
    }

    @Test
    @DisplayName("2. Duplicate earning creation is idempotent and prevented")
    void testDuplicateEarningPrevention() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        OrderItem item = order.getItems().get(0);

        teacherFinanceService.recordEarningForOrderItem(item);
        teacherFinanceService.recordEarningForOrderItem(item);
        teacherFinanceService.recordEarningForOrderItem(item);

        List<TeacherEarning> earnings = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId());
        assertThat(earnings).hasSize(1);
    }

    @Test
    @DisplayName("3. Teacher Balance Summary and Escrow Hold Period calculation")
    void testTeacherBalanceSummaryAndEscrowHoldPeriod() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        TeacherEarning earning = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId()).get(0);
        
        // Before hold period expires, it's pending
        earning.setStatus(EarningStatus.PENDING);
        earning.setAvailableAt(LocalDateTime.now().plusDays(7));
        teacherEarningRepository.save(earning);

        TeacherBalanceSummaryResponse summaryBefore = teacherFinanceService.getBalanceSummary(teacher.getId());
        assertThat(summaryBefore.getTotalEarned()).isEqualByComparingTo("239200.00");
        assertThat(summaryBefore.getPendingBalance()).isEqualByComparingTo("239200.00");
        assertThat(summaryBefore.getAvailableBalance()).isEqualByComparingTo("0.00");

        // Make earning available (hold period passed)
        earning.setStatus(EarningStatus.AVAILABLE);
        earning.setAvailableAt(LocalDateTime.now().minusMinutes(1));
        teacherEarningRepository.save(earning);

        TeacherBalanceSummaryResponse summaryAfter = teacherFinanceService.getBalanceSummary(teacher.getId());
        assertThat(summaryAfter.getTotalEarned()).isEqualByComparingTo("239200.00");
        assertThat(summaryAfter.getPendingBalance()).isEqualByComparingTo("0.00");
        assertThat(summaryAfter.getAvailableBalance()).isEqualByComparingTo("239200.00");
    }

    @Test
    @DisplayName("4. Withdrawal Request Balance Validation & Overdraft Prevention")
    void testWithdrawalRequestAndBalanceValidation() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        TeacherEarning earning = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId()).get(0);
        earning.setStatus(EarningStatus.AVAILABLE);
        earning.setAvailableAt(LocalDateTime.now().minusMinutes(1));
        teacherEarningRepository.save(earning);

        // Add bank account
        TeacherBankAccountRequest bankReq = TeacherBankAccountRequest.builder()
                .bankName("Vietcombank")
                .bankCode("VCB")
                .accountNumber("0123456789")
                .accountHolderName("TEACHER MASTER")
                .isDefault(true)
                .build();
        teacherFinanceService.addBankAccount(teacher.getId(), bankReq);

        // Attempt overdraft withdrawal
        CreateWithdrawalRequest overdraftReq = CreateWithdrawalRequest.builder()
                .amount(new BigDecimal("500000.00"))
                .build();

        assertThatThrownBy(() -> teacherFinanceService.requestWithdrawal(teacher.getId(), overdraftReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong du");

        // Valid withdrawal request
        CreateWithdrawalRequest validReq = CreateWithdrawalRequest.builder()
                .amount(new BigDecimal("100000.00"))
                .idempotencyKey("KEY_12345")
                .build();

        TeacherWithdrawalResponse withdrawal = teacherFinanceService.requestWithdrawal(teacher.getId(), validReq);
        assertThat(withdrawal.getAmount()).isEqualByComparingTo("100000.00");
        assertThat(withdrawal.getStatus()).isEqualTo("PENDING");
        assertThat(withdrawal.getAccountNumberMasked()).isEqualTo("******6789");

        // Available balance is reduced by committed withdrawal
        TeacherBalanceSummaryResponse summary = teacherFinanceService.getBalanceSummary(teacher.getId());
        assertThat(summary.getAvailableBalance()).isEqualByComparingTo("139200.00"); // 239,200 - 100,000
    }

    @Test
    @DisplayName("5. Withdrawal Idempotency check")
    void testWithdrawalIdempotency() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        TeacherEarning earning = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId()).get(0);
        earning.setStatus(EarningStatus.AVAILABLE);
        earning.setAvailableAt(LocalDateTime.now().minusMinutes(1));
        teacherEarningRepository.save(earning);

        teacherFinanceService.addBankAccount(teacher.getId(), TeacherBankAccountRequest.builder()
                .bankName("MBBank")
                .bankCode("MB")
                .accountNumber("9876543210")
                .accountHolderName("TEACHER MASTER")
                .build());

        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .amount(new BigDecimal("100000.00"))
                .idempotencyKey("IDEMPOTENT_WTD_001")
                .build();

        TeacherWithdrawalResponse resp1 = teacherFinanceService.requestWithdrawal(teacher.getId(), req);
        TeacherWithdrawalResponse resp2 = teacherFinanceService.requestWithdrawal(teacher.getId(), req);

        assertThat(resp1.getId()).isEqualTo(resp2.getId());
        assertThat(resp1.getWithdrawalCode()).isEqualTo(resp2.getWithdrawalCode());
    }

    @Test
    @DisplayName("6. Admin Payout Processing (Approve, Complete with Ref, Reject)")
    void testAdminPayoutLifecycle() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        TeacherEarning earning = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId()).get(0);
        earning.setStatus(EarningStatus.AVAILABLE);
        earning.setAvailableAt(LocalDateTime.now().minusMinutes(1));
        teacherEarningRepository.save(earning);

        teacherFinanceService.addBankAccount(teacher.getId(), TeacherBankAccountRequest.builder()
                .bankName("Techcombank")
                .bankCode("TCB")
                .accountNumber("190300001234")
                .accountHolderName("TEACHER MASTER")
                .build());

        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .amount(new BigDecimal("150000.00"))
                .build();

        TeacherWithdrawalResponse created = teacherFinanceService.requestWithdrawal(teacher.getId(), req);

        // Admin approves
        TeacherWithdrawalResponse approved = adminFinanceService.approveWithdrawal(created.getId(), "admin@nqd.edu.vn");
        assertThat(approved.getStatus()).isEqualTo("PROCESSING");

        // Admin completes
        AdminProcessWithdrawalRequest completeReq = AdminProcessWithdrawalRequest.builder()
                .referenceCode("FT2609068899")
                .build();
        TeacherWithdrawalResponse completed = adminFinanceService.completeWithdrawal(created.getId(), completeReq, "admin@nqd.edu.vn");
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getReferenceCode()).isEqualTo("FT2609068899");

        TeacherBalanceSummaryResponse summary = teacherFinanceService.getBalanceSummary(teacher.getId());
        assertThat(summary.getWithdrawnAmount()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("7. Refund Processing: Earning Reversal and Entitlement Revocation")
    void testOrderRefundAndReversalFlow() {
        Order order = createPaidOrder(new BigDecimal("299000.00"));
        entitlementActivationService.activateOrderFulfillment(order);

        // Verify active entitlement exists
        boolean hasAccessBefore = entitlementRepository.hasActiveEntitlement(
                student.getId(), EntitlementType.COURSE_ACCESS, paidCourse.getId(), LocalDateTime.now()
        );
        assertThat(hasAccessBefore).isTrue();

        // Process refund
        ProcessRefundRequest refundReq = ProcessRefundRequest.builder()
                .orderId(order.getId())
                .refundReason("Khach hang yeu cau hoan tien do nham lan")
                .build();

        RefundResponse refundResp = adminFinanceService.processRefund(refundReq, "admin@nqd.edu.vn");
        assertThat(refundResp.getStatus()).isEqualTo("REFUNDED");
        assertThat(refundResp.getReversedEarningsCount()).isEqualTo(1);
        assertThat(refundResp.getRevokedEntitlementsCount()).isEqualTo(1);

        // Verify earning is REVERSED
        TeacherEarning earning = teacherEarningRepository.findByOrderIdAndIsDeletedFalse(order.getId()).get(0);
        assertThat(earning.getStatus()).isEqualTo(EarningStatus.REVERSED);
        assertThat(earning.getReversalReason()).contains("Khach hang yeu cau");

        // Verify entitlement is revoked
        boolean hasAccessAfter = entitlementRepository.hasActiveEntitlement(
                student.getId(), EntitlementType.COURSE_ACCESS, paidCourse.getId(), LocalDateTime.now()
        );
        assertThat(hasAccessAfter).isFalse();

        // Verify teacher total earned excludes reversed
        TeacherBalanceSummaryResponse summary = teacherFinanceService.getBalanceSummary(teacher.getId());
        assertThat(summary.getTotalEarned()).isEqualByComparingTo("0.00");
        assertThat(summary.getReversedAmount()).isEqualByComparingTo("239200.00");
    }
}