package com.core.vdesk.domain.welcome.service;

import static java.time.Instant.now;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.core.vdesk.domain.welcome.dto.WelcomeBillingTargetDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final WelcomeBillingService welcomeBillingService;

    @Scheduled(cron = "0 0 */12 * * *", zone = "Asia/Seoul") // 12시간마다 한번
    public void welcomeBillingCharge() {
        log.info("자동결제 스케줄러 실행");
        // 만료 정리(시간 경과로 인한 상태 불일치 보정), 웰컴 페이팔 모두 여기서 수행
        try {
            int swept = welcomeBillingService.expireSweep(now());
            log.info("웰컴 만료정리 완료 수 swept={}", swept);
        } catch (Exception e) {
            log.error("웰컴 만료정리 실패", e);
        }

        // 웰컴 빌링결제 타겟
        List<WelcomeBillingTargetDto> targets = welcomeBillingService.findBillingTargets(now());

        for (WelcomeBillingTargetDto target : targets) {
            try {
                welcomeBillingService.charge(target);
            } catch (Exception e) {
                log.error("자동결제 실패: user={}, key={}",
                        target.getUserId(), target.getBillingKey(), e);
            }
        }
    }

}
