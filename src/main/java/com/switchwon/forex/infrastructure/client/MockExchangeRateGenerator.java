package com.switchwon.forex.infrastructure.client;

import com.switchwon.forex.domain.common.Currency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock 환율 생성기
 *
 * 사용 시점: 외부 API 호출 실패 시 Fallback 전용
 * - 정상 동작 시에는 사용되지 않음 (외부 API 실제 데이터 사용)
 * - 과제 명세: "외부 API 연동이 어려울 경우, 현실적인 시세 범위 내에서 Mock 데이터 생성"
 *
 * 랜덤 변동 적용 이유:
 * - Fallback 상황에서도 1분마다 스케줄러가 다른 값을 DB에 저장함을 확인 가능
 * - 과제 명세: "스케줄러의 동작 확인을 위해 임의의 랜덤값 추가 무방"
 * - 변동폭 ±1%: 현실적 범위 유지
 */
@Slf4j
@Component
public class MockExchangeRateGenerator {

    private final Random random = new Random();

    // 2026년 4월 기준 현실적 매매기준율 (KRW 기준, JPY는 100엔 단위)
    private static final Map<Currency, BigDecimal> BASE_RATES = new EnumMap<>(Currency.class);

    static {
        BASE_RATES.put(Currency.USD, new BigDecimal("1380.00"));
        BASE_RATES.put(Currency.JPY, new BigDecimal("910.00"));
        BASE_RATES.put(Currency.CNY, new BigDecimal("190.00"));
        BASE_RATES.put(Currency.EUR, new BigDecimal("1530.00"));
    }

    private static final double FLUCTUATION_RATE = 0.01; // ±1%

    /**
     * 현실적 시세 범위 내 랜덤 매매기준율 생성
     * API 실패 시 Fallback으로만 호출됨
     */
    public Map<Currency, BigDecimal> generateMockRates() {
        log.info("Mock 환율 데이터 생성 (외부 API Fallback)");
        Map<Currency, BigDecimal> mockRates = new EnumMap<>(Currency.class);

        for (Map.Entry<Currency, BigDecimal> entry : BASE_RATES.entrySet()) {
            Currency currency = entry.getKey();
            BigDecimal baseRate = entry.getValue();

            double fluctuation = 1 + (random.nextDouble() * 2 - 1) * FLUCTUATION_RATE;
            BigDecimal variedRate = baseRate
                .multiply(BigDecimal.valueOf(fluctuation))
                .setScale(2, RoundingMode.HALF_UP);

            mockRates.put(currency, variedRate);
            log.debug("Mock 환율: {} = {} KRW", currency, variedRate);
        }

        return mockRates;
    }
}
