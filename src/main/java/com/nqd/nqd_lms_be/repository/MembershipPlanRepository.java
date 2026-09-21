package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID>, JpaSpecificationExecutor<MembershipPlan> {

    Optional<MembershipPlan> findByPlanCodeAndIsDeletedFalse(String planCode);

    List<MembershipPlan> findByStatusAndIsDeletedFalseOrderByPriceAsc(ProductStatus status);

    List<MembershipPlan> findByUserTypeAndStatusAndActiveTrueAndIsDeletedFalseOrderByPriceAsc(
            PlanUserType userType, ProductStatus status
    );

    List<MembershipPlan> findByUserTypeInAndStatusAndActiveTrueAndIsDeletedFalseOrderByPriceAsc(
            List<PlanUserType> userTypes, ProductStatus status
    );

    boolean existsByPlanCode(String planCode);
}
