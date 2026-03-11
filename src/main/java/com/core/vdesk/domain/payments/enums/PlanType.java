package com.core.vdesk.domain.payments.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanType {
    FREE_PLAN(1, 1),
    BASIC_PLAN(10, 1),
    STANDARD_PLAN(20, 1),
    BUSINESS_PLAN(Integer.MAX_VALUE, 5),
    SOLO_PLAN(10, 1),
    PRO_PLAN(20, 1),
    TEAM_PLAN(50, 3),
    TEST_PLAN(3, 1);




    private final int maxDevices;
    private final int basicSessions;

    public boolean isUnlimited() {
        return maxDevices == Integer.MAX_VALUE;
    }
}
