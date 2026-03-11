package com.core.vdesk.domain.emails;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling   // 스케쥴링 활성화
public class InMemoryEmailVerificationStore {

    /**
     * 이메일별로 보관되는 엔트리
     */
    private static class Entry {
        String code;            // 인증코드
        Instant expiresAt;      // 만료시각
        boolean verified;       // 검증 통과 여부
        Entry(String code, Duration ttl) {
            this.code = code;
            this.expiresAt = Instant.now().plus(ttl);
            this.verified = false;
        }
    }

    /**
     * 메모리 내 저장소
     * key = 이메일, value = Entry
     *  ConcurrentHashMap 으로 멀티스레드 환경에서의 put/get/remove 안전성 확보
     *  내부적으로 락을 분할해서 처리하기 때문에 HashMap보다 병렬 처리에 유리
     */
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * 인증코드 저장(또는 덮어쓰기).
     * @param email 대상 이메일
     * @param code 발급된 인증코드
     * @param ttl 유효기간(10분)
     * 동작:
     * 동일 이메일 키에 대해 새 Entry 로 덮어쓴다.
     * 기존 코드가 있었다면 무효화되는 효과가 있다(가장 마지막 코드만 유효).
     */
    public void save(String email, String code, Duration ttl) {
        store.put(email, new Entry(code, ttl));
        // 운영환경에서는 삭제 필요
        log.info("[EmailVerificationStore] saved email={}, code={}, ttl={}초 남음",
                email, code, ttl.toSeconds());
    }

    /**
     * 인증코드 일치 검증
     * 일치하면 verified=true
     * @param email 이메일(키)
     * @param code  사용자 입력 코드
     * @return 일치 및 아직 만료 전이면 true, 그 외 false
     * 절차:
     *  1) 해당 이메일의 엔트리를 가져온다. 없으면 false.
     *  2) 현재 시각이 만료시각을 지났다면 remove 하고 false.
     *  3) 코드 일치 시 verified=true 로 설정하고 true 반환.
     *  4) 코드 불일치면 false.
     */
    public boolean checkAndMarkVerified(String email, String code) {
        Entry e = store.get(email);
        if (e == null) return false;
        // 만료되었으면 제거 후 실패
        if (Instant.now().isAfter(e.expiresAt)) {
            store.remove(email);
            return false;
        }
        // 코드 일치 → 검증 완료 true
        if (e.code.equals(code)) {
            e.verified = true;
            return true;
        }
        return false;
    }

    /**
     * 회원가입 최종단계에서 사용하는 확인 로직
     * 특정 이메일이 "검증 완료" 상태이면서 "아직 만료 전"인지 확인한다.
     * @param email 이메일
     * @return 검증 완료 && 유효기간 내 → true, 그 외 false
     */
    public boolean isVerified(String email) {
        Entry e = store.get(email);
        return e != null && e.verified && Instant.now().isBefore(e.expiresAt);
    }

    /**
     * 주기적 만료 데이터 정리 로직
     * fixedDelay = 600_000(ms) → 이전 실행 종료 10분 후 다시 실행. (대략 10분 간격으로 만료된 엔트리를 스캔하여 제거)
     * @EnableScheduling 이 붙어 있어야 스케줄링이 활성화된다.
     */
    @Scheduled(fixedDelay = 600_000)
    public void cleanUpExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }
}

