package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByTransactionCodeAndIsDeletedFalse(String transactionCode);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByProviderAndProviderTransactionIdAndIsDeletedFalse(String provider, String providerTransactionId);

    List<PaymentTransaction> findByOrderIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID orderId);

    Page<PaymentTransaction> findByOrderUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<PaymentTransaction> findByStatusAndIsDeletedFalse(PaymentStatus status, Pageable pageable);

    boolean existsByTransactionCode(String transactionCode);

    @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.status IN ('FAILED', 'CANCELLED') AND p.isDeleted = false")
    long countFailedPayments();

    @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.status IN ('PAID', 'SUCCESS') AND p.isDeleted = false")
    long countSuccessfulPayments();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status IN ('PAID', 'SUCCESS') AND p.paidAt >= :start AND p.paidAt <= :end AND p.isDeleted = false")
    BigDecimal sumPaymentAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
