package com.switchwon.forex.application.order;

import com.switchwon.forex.application.exchangerate.ExchangeRateCalculator;
import com.switchwon.forex.application.exchangerate.ExchangeRateNotFoundException;
import com.switchwon.forex.application.order.dto.OrderDto;
import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;
import com.switchwon.forex.domain.exchangerate.ExchangeRateRepository;
import com.switchwon.forex.domain.order.Order;
import com.switchwon.forex.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 외화 주문 서비스
 *
 * 주문 방향별 적용 환율:
 * - KRW → 외화 (매수): buyRate  (+5% 가산) - 고객이 외화를 사는 가격, 은행에 유리
 * - 외화 → KRW (매도): sellRate (-5% 차감) - 고객이 외화를 파는 가격, 은행에 유리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * 외화 주문 처리
     *
     * @Transactional 이유:
     * 환율 조회 + 주문 저장을 하나의 트랜잭션으로 묶어
     * 환율은 읽혔으나 주문이 저장 안 되는 부정합 상황 방지
     */
    @Transactional
    public OrderDto placeOrder(BigDecimal forexAmount, Currency fromCurrency, Currency toCurrency) {
        validateOrderCurrencies(fromCurrency, toCurrency);

        // KRW가 아닌 쪽이 외화
        Currency foreignCurrency = fromCurrency.isForeign() ? fromCurrency : toCurrency;

        ExchangeRate latestRate = exchangeRateRepository
            .findTop1ByCurrencyOrderByCreatedAtDesc(foreignCurrency)
            .orElseThrow(() -> new ExchangeRateNotFoundException(foreignCurrency));

        final Order order;
        if (fromCurrency == Currency.KRW) {
            // Case A: KRW → 외화 매수 (buyRate 적용)
            BigDecimal buyRate = latestRate.getBuyRate();
            BigDecimal krwAmount = ExchangeRateCalculator.calcKrwFromForex(forexAmount, buyRate);
            order = Order.of(krwAmount, Currency.KRW, forexAmount, toCurrency, buyRate);
        } else {
            // Case B: 외화 → KRW 매도 (sellRate 적용)
            BigDecimal sellRate = latestRate.getSellRate();
            BigDecimal krwAmount = ExchangeRateCalculator.calcKrwToForex(forexAmount, sellRate);
            order = Order.of(forexAmount, fromCurrency, krwAmount, Currency.KRW, sellRate);
        }

        Order saved = orderRepository.save(order);
        log.info("주문 완료 id={} | {} {} -> {} {}",
            saved.getId(), saved.getFromAmount(), saved.getFromCurrency(),
            saved.getToAmount(), saved.getToCurrency());

        return OrderDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrderList() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderDto::fromWithId)
            .toList();
    }

    /**
     * 허용 조합: KRW↔외화만 (외화↔외화, KRW↔KRW 불가)
     */
    private void validateOrderCurrencies(Currency from, Currency to) {
        boolean valid = (from == Currency.KRW && to.isForeign())
                     || (from.isForeign() && to == Currency.KRW);
        if (!valid) {
            throw new InvalidOrderException(
                String.format("지원하지 않는 통화 조합: %s -> %s. KRW <-> 외화(USD/JPY/CNY/EUR) 조합만 허용됩니다.", from, to)
            );
        }
    }
}
