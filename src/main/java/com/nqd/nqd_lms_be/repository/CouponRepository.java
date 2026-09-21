package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeAndIsDeletedFalse(String code);

    Optional<Coupon> findByCodeAndIsActiveTrueAndIsDeletedFalse(String code);

    boolean existsByCode(String code);
}
