package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Entitlement;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    List<Entitlement> findByUserIdAndIsDeletedFalse(UUID userId);

    List<Entitlement> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, EntitlementStatus status);

    List<Entitlement> findBySourceOrderIdAndIsDeletedFalse(UUID sourceOrderId);
    List<Entitlement> findBySourceSubscriptionIdAndIsDeletedFalse(UUID sourceSubscriptionId);

    boolean existsByUserIdAndEntitlementTypeAndStatusAndIsDeletedFalse(
            UUID userId, EntitlementType entitlementType, EntitlementStatus status
    );

    Optional<Entitlement> findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
            UUID userId, EntitlementType entitlementType, UUID targetEntityId, EntitlementStatus status
    );

    @Query("SELECT e FROM Entitlement e WHERE e.user.id = :userId AND e.entitlementType = :type AND e.targetEntityId = :targetId AND e.status = 'ACTIVE' AND e.validFrom <= :now AND (e.validUntil IS NULL OR e.validUntil > :now) AND e.isDeleted = false")
    Optional<Entitlement> findActiveEntitlement(
            @Param("userId") UUID userId,
            @Param("type") EntitlementType type,
            @Param("targetId") UUID targetId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entitlement e WHERE e.user.id = :userId AND e.entitlementType = :type AND e.targetEntityId = :targetId AND e.status = 'ACTIVE' AND e.validFrom <= :now AND (e.validUntil IS NULL OR e.validUntil > :now) AND e.isDeleted = false")
    boolean hasActiveEntitlement(
            @Param("userId") UUID userId,
            @Param("type") EntitlementType type,
            @Param("targetId") UUID targetId,
            @Param("now") LocalDateTime now
    );
}
