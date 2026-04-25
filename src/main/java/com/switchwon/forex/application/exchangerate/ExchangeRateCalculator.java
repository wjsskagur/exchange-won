package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.domain.common.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율 계산 유틸리티 (순수 계산 로직)
 *
 * JPY 100엔 단위 처리 전략:
 * - DB 저장 및 API 응답: 100엔 기준 (e.g. tradeStanRate=910.00, buyRate=955.50)
 * - 주문 금액 계산: 1엔 기준으로 변환 후 계산 (toUnitRate)
 *   이유: forexAmount=500은 500엔을 의미하므로
 *         500 × 955.50(100엔 기준) = 477,750 → 잘못된 계산
 *         500 × 9.555(1엔 기준)   = 4,777   → 올바른 계산
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
     * API 응답값(1 KRW 기준 외화량)을 매매기준율(외화 기준 KRW)로 변환
     *
     * open.er-api.com /KRW 응답: { "USD": 0.000724, "JPY": 0.10989, ... }
     * 의미: 1 KRW = 0.000724 USD → 역수 → 1 USD = 1381.21 KRW
     *
     * JPY 특례 (과제 요구사항: 100엔 단위 표시):
     * 1 JPY = 9.10 KRW → × 100 → 100 JPY = 910.00 KRW (DB/응답 저장값)
     */
    public static BigDecimal toTradeStanRate(BigDecimal apiRate, Currency currency) {
        if (apiRate == null || apiRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("API 환율값이 0이거나 null: " + currency);
        }

        BigDecimal krwPerUnit = BigDecimal.ONE.divide(apiRate, 10, RoundingMode.HALF_UP);

        if (currency.isJpy()) {
            krwPerUnit = krwPerUnit.multiply(JPY_UNIT);  // 1엔 → 100엔 기준으로 변환
        }

        return krwPerUnit.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * JPY 100엔 기준 환율을 1엔 기준으로 변환
     *
     * 주문 계산 시 사용:
     * - buyRate/sellRate는 100엔 기준으로 DB에 저장됨
     * - forexAmount는 실제 엔화 단위 (e.g. 500엔)
     * - 계산 시 1엔 기준 환율이 필요
     *
     * e.g. buyRate=955.50 (100엔 기준) → 9.5550 (1엔 기준)
     */
    public static BigDecimal toUnitRate(BigDecimal hundredYenRate) {
        return hundredYenRate.divide(JPY_UNIT, 10, RoundingMode.HALF_UP);
    }

    /**
     * KRW → 외화 매수 시 필요한 원화 금액 계산
     * fromAmount(KRW) = forexAmount × effectiveRate → Floor
     * (JPY의 경우 effectiveRate는 1엔 기준으로 변환된 값)
     */
    public static BigDecimal calcKrwFromForex(BigDecimal forexAmount, BigDecimal effectiveRate) {
        return forexAmount
            .multiply(effectiveRate)
            .setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 외화 → KRW 매도 시 받을 원화 금액 계산
     * toAmount(KRW) = forexAmount × effectiveRate → Floor
     * (JPY의 경우 effectiveRate는 1엔 기준으로 변환된 값)
     */
    public static BigDecimal calcKrwToForex(BigDecimal forexAmount, BigDecimal effectiveRate) {
        return forexAmount
            .multiply(effectiveRate)
            .setScale(0, RoundingMode.FLOOR);
    }
}
