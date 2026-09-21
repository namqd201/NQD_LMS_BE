package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.membership.dto.CreateMembershipPlanRequest;
import com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse;
import com.nqd.nqd_lms_be.membership.dto.UpdateMembershipPlanRequest;

import java.util.List;
import java.util.UUID;

public interface MembershipPlanService {

    List<MembershipPlanResponse> getPublicPlans(PlanUserType userType);

    List<MembershipPlanResponse> getAllPlansAdmin();

    MembershipPlanResponse getPlanById(UUID planId);

    MembershipPlan getPlanEntityById(UUID planId);

    MembershipPlan getPlanEntityByCode(String planCode);

    MembershipPlanResponse createPlan(CreateMembershipPlanRequest request);

    MembershipPlanResponse updatePlan(UUID planId, UpdateMembershipPlanRequest request);

    MembershipPlanResponse togglePlanStatus(UUID planId, boolean active);

    void initDefaultPlans();
}
