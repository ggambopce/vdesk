package com.core.vdesk.domain.payments.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.core.vdesk.domain.payments.entity.UserPlan;
import com.core.vdesk.domain.users.Users;

public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {

    Optional<UserPlan> findByUser_UserId(Long userId);

    Optional<UserPlan> findTopByUserOrderByCreatedAtDesc(Users user);

    @Query("""
        select up
        from UserPlan up
        where up.expiresAt is not null
          and up.expiresAt <= :now
    """)
    List<UserPlan> findExpiredPlans(@Param("now") Instant now);

    @Query("select up from UserPlan up where up.nextChargeDate <= :now")
    List<UserPlan> findDuePlans(@Param("now") Instant now);
}
