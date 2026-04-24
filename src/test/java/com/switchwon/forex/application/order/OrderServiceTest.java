package com.switchwon.forex.application.order;

import com.switchwon.forex.application.exchangerate.ExchangeRateNotFoundException;
import com.switchwon.forex.application.order.dto.OrderDto;
import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;
import com.switchwon.forex.domain.exchangerate.ExchangeRateRepository;
import com.switchwon.forex.domain.order.Order;
import com.switchwon.forex.domain.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * OrderService 단위 테스트
 *
 * @ExtendWith(MockitoExtension.class) 사용 이유:
 * - Spring 컨텍스트 로딩 없이 빠른 단위 테스트
 * - 의존성(Repository)을 Mock으로 교체하여 순수 비즈니스 로직만 검증
 * - @SpringBootTest는 전체 컨텍스트 로딩이라 속도가 느림
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Case A: KRW → USD 매수 시 buyRate 적용, KRW 금액 floor 처리")
    void placeOrder_buyUsd_applyBuyRate() {
        // given
        BigDecimal tradeStanRate = new BigDecimal("1477.45");
        BigDecimal buyRate = new BigDecimal("1551.32");   // 1477.45 * 1.05
        BigDecimal sellRate = new BigDecimal("1403.58");  // 1477.45 * 0.95

        ExchangeRate mockRate = ExchangeRate.of(Currency.USD, tradeStanRate, buyRate, sellRate);
        given(exchangeRateRepository.findTop1ByCurrencyOrderByCreatedAtDesc(Currency.USD))
            .willReturn(Optional.of(mockRate));
        given(orderRepository.save(any(Order.class)))
            .willAnswer(inv -> inv.getArgument(0));  // 전달받은 Order 그대로 반환

        // when
        OrderDto result = orderService.placeOrder(
            new BigDecimal("200"), Currency.KRW, Currency.USD);

        // then
        assertThat(result.fromCurrency()).isEqualTo("KRW");
        assertThat(result.toCurrency()).isEqualTo("USD");
        assertThat(result.toAmount()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(result.tradeRate()).isEqualByComparingTo(buyRate);

        // 200 × 1551.32 = 310264.0 → floor = 310264
        assertThat(result.fromAmount()).isEqualByComparingTo(new BigDecimal("310264"));
    }

    @Test
    @DisplayName("Case B: USD → KRW 매도 시 sellRate 적용, KRW 금액 floor 처리")
    void placeOrder_sellUsd_applySellRate() {
        // given
        BigDecimal tradeStanRate = new BigDecimal("1477.45");
        BigDecimal buyRate = new BigDecimal("1551.32");
        BigDecimal sellRate = new BigDecimal("1403.58");

        ExchangeRate mockRate = ExchangeRate.of(Currency.USD, tradeStanRate, buyRate, sellRate);
        given(exchangeRateRepository.findTop1ByCurrencyOrderByCreatedAtDesc(Currency.USD))
            .willReturn(Optional.of(mockRate));
        given(orderRepository.save(any(Order.class)))
            .willAnswer(inv -> inv.getArgument(0));

        // when
        OrderDto result = orderService.placeOrder(
            new BigDecimal("133"), Currency.USD, Currency.KRW);

        // then
        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.toCurrency()).isEqualTo("KRW");
        assertThat(result.fromAmount()).isEqualByComparingTo(new BigDecimal("133"));
        assertThat(result.tradeRate()).isEqualByComparingTo(sellRate);

        // 133 × 1403.58 = 186676.14 → floor = 186676
        assertThat(result.toAmount()).isEqualByComparingTo(new BigDecimal("186676"));
    }

    @Test
    @DisplayName("환율 데이터 없을 때 ExchangeRateNotFoundException 발생")
    void placeOrder_noExchangeRate_throwsException() {
        // given
        given(exchangeRateRepository.findTop1ByCurrencyOrderByCreatedAtDesc(Currency.USD))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            orderService.placeOrder(new BigDecimal("100"), Currency.KRW, Currency.USD)
        ).isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    @DisplayName("외화↔외화 조합(USD→JPY)은 InvalidOrderException 발생")
    void placeOrder_foreignToForeign_throwsException() {
        assertThatThrownBy(() ->
            orderService.placeOrder(new BigDecimal("100"), Currency.USD, Currency.JPY)
        ).isInstanceOf(InvalidOrderException.class)
         .hasMessageContaining("지원하지 않는 통화 조합");
    }

    @Test
    @DisplayName("KRW→KRW 주문은 InvalidOrderException 발생")
    void placeOrder_krwToKrw_throwsException() {
        assertThatThrownBy(() ->
            orderService.placeOrder(new BigDecimal("10000"), Currency.KRW, Currency.KRW)
        ).isInstanceOf(InvalidOrderException.class);
    }
}
