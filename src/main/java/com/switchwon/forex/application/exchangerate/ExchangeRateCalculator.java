package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.domain.common.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율 계산 유틸리티 (순수 계산 로직)
 *
 * Static Utility Class로 설계한 이유:
 * - 외부 의존성 없는 순수 계산 로직 → 인스턴스화 불필요
 * - 테스트 시 Spring 컨텍스트 없이 직접 단위 테스트 가능
 *
 * BigDecimal 사용 이유:
 * - double/float은 부동소수점 오차 발생 (0.1 + 0.2 = 0.30000000000000004)
 * - 금융 계산은 정확한 십진수 연산이 필수
 *
 * 정밀도 전략:
 * - 중간 계산: scale=10 (정밀도 유지)
 * - 환율 최종값: scale=2, HALF_UP  (과제: 소수점 둘째 자리 반올림)
 * - KRW 금액: scale=0, FLOOR       (과제: 원화 소수점 이하 버림)
 */
public class ExchangeRateCalculator {

    private static final BigDecimal BUY_RATE_MULTIPLIER  = new BigDecimal("1.05");
    private static final BigDecimal SELL_RATE_MULTIPLIER = new BigDecimal("0.95");
    private static final BigDecimal JPY_UNIT             = new BigDecimal("100");

    private ExchangeRateCalculator() {}

    /**
     * 전신환 매입율 = 매매기준율 × 1.05, 소수점 둘째 자리 반올림
     */
    public static BigDecimal calcBuyRate(BigDecimal tradeStanRate) {
        return tradeStanRate
            .multiply(BUY_RATE_MULTIPLIER)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 전신환 매도율 = 매매기준율 × 0.95, 소수점 둘째 자리 반올림
     */
    public static BigDecimal calcSellRate(BigDecimal tradeStanRate) {
        return tradeStanRate
            .multiply(SELL_RATE_MULTIPLIER)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * API 응답값(1 KRW 기준 외화량)을 매매기준율(1외화 기준 KRW)로 변환
     *
     * open.er-api.com /KRW 응답: { "USD": 0.000724, ... }
     * 의미: 1 KRW = 0.000724 USD
     * 역수: 1 USD = 1 / 0.000724 = 1381.21 KRW
     *
     * JPY 특례 (과제 요구사항):
     * API는 1엔 기준이나, 국내 표준인 100엔 단위로 환산
     * 1 JPY = 9.10 KRW → × 100 → 100 JPY = 910.00 KRW
     */
    public static BigDecimal toTradeStanRate(BigDecimal apiRate, Currency currency) {
        if (apiRate == null || apiRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("API 환율값이 0이거나 null: " + currency);
        }

        BigDecimal krwPerUnit = BigDecimal.ONE.divide(apiRate, 10, RoundingMode.HALF_UP);

        if (currency.isJpy()) {
            krwPerUnit = krwPerUnit.multiply(JPY_UNIT);
        }

        return krwPerUnit.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * KRW → 외화 매수 시 필요한 원화 금액 계산
     * fromAmount(KRW) = forexAmount × buyRate → Floor
     */
    public static BigDecimal calcKrwFromForex(BigDecimal forexAmount, BigDecimal buyRate) {
        return forexAmount
            .multiply(buyRate)
            .setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 외화 → KRW 매도 시 받을 원화 금액 계산
     * toAmount(KRW) = forexAmount × sellRate → Floor
     */
    public static BigDecimal calcKrwToForex(BigDecimal forexAmount, BigDecimal sellRate) {
        return forexAmount
            .multiply(sellRate)
            .setScale(0, RoundingMode.FLOOR);
    }
}
