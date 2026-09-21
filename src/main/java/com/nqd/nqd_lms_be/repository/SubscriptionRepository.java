package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Subscription;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

    List<Subscription> findByUserIdAndIsDeletedFalseOrderByStartDateDesc(UUID userId);

    List<Subscription> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.membershipPlan WHERE s.status = 'ACTIVE' AND s.startDate <= :now AND (s.endDate IS NULL OR s.endDate > :now) AND s.isDeleted = false")
    List<Subscription> findAllActiveSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.membershipPlan WHERE s.user.id = :userId AND s.status = 'ACTIVE' AND s.startDate <= :now AND (s.endDate IS NULL OR s.endDate > :now) AND s.isDeleted = false ORDER BY s.endDate DESC")
    List<Subscription> findActiveSubscriptionsByUserList(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    default Optional<Subscription> findActiveSubscriptionByUser(UUID userId, LocalDateTime now) {
        List<Subscription> list = findActiveSubscriptionsByUserList(userId, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE' AND s.isDeleted = false")
    List<Subscription> findActiveSubscriptionsByUserWithLock(@Param("userId") UUID userId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate IS NOT NULL AND s.endDate <= :now AND s.isDeleted = false")
    List<Subscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);

    Page<Subscription> findByStatusAndIsDeletedFalse(SubscriptionStatus status, Pageable pageable);
}
