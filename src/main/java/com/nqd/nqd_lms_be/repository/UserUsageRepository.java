package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.UserUsageRecord;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserUsageRepository extends JpaRepository<UserUsageRecord, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserUsageRecord u WHERE u.user.id = :userId AND u.featureKey = :featureKey AND u.periodKey = :periodKey AND u.isDeleted = false")
    Optional<UserUsageRecord> findByUserIdAndFeatureKeyAndPeriodKeyWithLock(
            @Param("userId") UUID userId,
            @Param("featureKey") FeatureKey featureKey,
            @Param("periodKey") String periodKey
    );

    Optional<UserUsageRecord> findByUserIdAndFeatureKeyAndPeriodKeyAndIsDeletedFalse(
            UUID userId,
            FeatureKey featureKey,
            String periodKey
    );

    List<UserUsageRecord> findByUserIdAndIsDeletedFalse(UUID userId);
}
