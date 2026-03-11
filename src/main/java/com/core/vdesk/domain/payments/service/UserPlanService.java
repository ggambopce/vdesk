package com.core.vdesk.domain.payments.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.payments.entity.UserPlan;
import com.core.vdesk.domain.payments.enums.PaymentStatus;
import com.core.vdesk.domain.payments.enums.PlanType;
import com.core.vdesk.domain.payments.repository.UserPlanRepository;
import com.core.vdesk.domain.users.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPlanService {
    private final UserPlanRepository userPlanRepository;

    @Transactional
    public void ensureFreePlanExists(Users user) {
        UserPlan latestPlan = userPlanRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null);
        if (latestPlan == null) {
            UserPlan plan = new UserPlan();
            plan.setUser(user);
            plan.setPlanType(PlanType.FREE_PLAN);
            plan.setPaymentStatus(PaymentStatus.ACTIVE);
            userPlanRepository.save(plan);
        }
    }
}
