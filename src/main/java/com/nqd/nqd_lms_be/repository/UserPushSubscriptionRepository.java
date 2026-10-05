package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.UserPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPushSubscriptionRepository extends JpaRepository<UserPushSubscription, UUID> {
    List<UserPushSubscription> findByUserId(UUID userId);
    List<UserPushSubscription> findByUserIdIn(List<UUID> userIds);
    Optional<UserPushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
