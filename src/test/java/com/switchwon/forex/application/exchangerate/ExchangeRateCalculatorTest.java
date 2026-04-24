package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.domain.common.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExchangeRateCalculator 단위 테스트
 *
 * 순수 계산 로직이므로 Spring 컨텍스트 불필요 → 빠른 실행
 * 금융 계산의 정확성이 핵심이므로 경계값 테스트 포함
 */
@DisplayName("환율 계산기 단위 테스트")
class ExchangeRateCalculatorTest {

    @Test
    @DisplayName("매입율 = 매매기준율 × 1.05, 소수점 2자리 반올림")
    void calcBuyRate_success() {
        BigDecimal tradeStanRate = new BigDecimal("1477.45");
        BigDecimal buyRate = ExchangeRateCalculator.calcBuyRate(tradeStanRate);
        // 1477.45 × 1.05 = 1551.3225 → 1551.32
        assertThat(buyRate).isEqualByComparingTo(new BigDecimal("1551.32"));
    }

    @Test
    @DisplayName("매도율 = 매매기준율 × 0.95, 소수점 2자리 반올림")
    void calcSellRate_success() {
        BigDecimal tradeStanRate = new BigDecimal("1477.45");
        BigDecimal sellRate = ExchangeRateCalculator.calcSellRate(tradeStanRate);
        // 1477.45 × 0.95 = 1403.5775 → 1403.58
        assertThat(sellRate).isEqualByComparingTo(new BigDecimal("1403.58"));
    }

    @Test
    @DisplayName("JPY는 100엔 단위로 환산")
    void toTradeStanRate_jpy_100unit() {
        // 1 KRW = 0.10989 JPY → 1 JPY = 9.1002 KRW → 100 JPY = 910.02 KRW
        BigDecimal apiRate = new BigDecimal("0.10989");
        BigDecimal result = ExchangeRateCalculator.toTradeStanRate(apiRate, Currency.JPY);
        // 1/0.10989 × 100 = 910.00... (소수점 처리)
        assertThat(result.scale()).isEqualTo(2);
        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("USD는 1달러 단위 (×100 없음)")
    void toTradeStanRate_usd_1unit() {
        // 1 KRW = 0.000724 USD → 1 USD = 1381.22 KRW
        BigDecimal apiRate = new BigDecimal("0.000724");
        BigDecimal result = ExchangeRateCalculator.toTradeStanRate(apiRate, Currency.USD);
        assertThat(result).isGreaterThan(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("KRW→외화 매수: forexAmount × buyRate, 원화 Floor 처리")
    void calcKrwFromForex_floor() {
        BigDecimal forexAmount = new BigDecimal("200");
        BigDecimal buyRate = new BigDecimal("1480.43");
        BigDecimal result = ExchangeRateCalculator.calcKrwFromForex(forexAmount, buyRate);
        // 200 × 1480.43 = 296086.0 → floor = 296086
        assertThat(result).isEqualByComparingTo(new BigDecimal("296086"));
        assertThat(result.scale()).isEqualTo(0);
    }

    @Test
    @DisplayName("외화→KRW 매도: forexAmount × sellRate, 원화 Floor 처리")
    void calcKrwToForex_floor() {
        BigDecimal forexAmount = new BigDecimal("133");
        BigDecimal sellRate = new BigDecimal("1474.47");
        BigDecimal result = ExchangeRateCalculator.calcKrwToForex(forexAmount, sellRate);
        // 133 × 1474.47 = 196104.51 → floor = 196104
        assertThat(result).isEqualByComparingTo(new BigDecimal("196104"));
        assertThat(result.scale()).isEqualTo(0);
    }

    @Test
    @DisplayName("API 환율값이 0이면 예외 발생")
    void toTradeStanRate_zeroApiRate_throws() {
        assertThatThrownBy(() ->
            ExchangeRateCalculator.toTradeStanRate(BigDecimal.ZERO, Currency.USD)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
