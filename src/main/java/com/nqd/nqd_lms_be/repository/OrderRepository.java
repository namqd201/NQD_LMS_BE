package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Order;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.orderCode = :orderCode AND o.isDeleted = false")
    Optional<Order> findByOrderCodeWithDetails(@Param("orderCode") String orderCode);

    Optional<Order> findByOrderCodeAndIsDeletedFalse(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderCode = :orderCode AND o.isDeleted = false")
    Optional<Order> findByOrderCodeWithLock(@Param("orderCode") String orderCode);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.user.id = :userId AND (:status IS NULL OR o.status = :status) AND o.isDeleted = false ORDER BY o.placedAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND (:status IS NULL OR o.status = :status) AND o.isDeleted = false")
    Page<Order> findByUserIdAndStatusAndIsDeletedFalseOrderByPlacedAtDesc(@Param("userId") UUID userId, @Param("status") OrderStatus status, Pageable pageable);

    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.user.id = :userId AND o.isDeleted = false ORDER BY o.placedAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.isDeleted = false")
    Page<Order> findByUserIdAndIsDeletedFalseOrderByPlacedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    Page<Order> findByStatusAndIsDeletedFalse(OrderStatus status, Pageable pageable);

    boolean existsByOrderCode(String orderCode);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PAID', 'COMPLETED') AND o.isDeleted = false")
    long countPaidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING' AND o.isDeleted = false")
    long countPendingOrders();

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'COMPLETED') AND o.completedAt >= :start AND o.completedAt <= :end AND o.isDeleted = false")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
