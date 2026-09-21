package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.OAuthAccount;
import com.nqd.nqd_lms_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    List<OAuthAccount> findByUser(User user);
}
