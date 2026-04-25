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
 * JPY 100엔 단위 주의사항:
 * - DB/응답의 buyRate, sellRate는 100엔 기준으로 저장됨 (e.g. 955.50 = 100엔당 KRW)
 * - 주문 계산 시 forexAmount는 실제 엔화 금액 (e.g. forexAmount=500 → 500엔)
 * - 따라서 JPY 주문 시 실제 적용 환율(tradeRate)은 1엔 기준으로 변환 후 계산
 *   500엔 × (955.50 / 100) = 500 × 9.555 = 4,777원
 *
 * 주문 방향별 적용 환율:
 * - KRW → 외화 (매수): buyRate  (+5% 가산)
 * - 외화 → KRW (매도): sellRate (-5% 차감)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public OrderDto placeOrder(BigDecimal forexAmount, Currency fromCurrency, Currency toCurrency) {
        validateOrderCurrencies(fromCurrency, toCurrency);

        Currency foreignCurrency = fromCurrency.isForeign() ? fromCurrency : toCurrency;

        ExchangeRate latestRate = exchangeRateRepository
            .findTop1ByCurrencyOrderByCreatedAtDesc(foreignCurrency)
            .orElseThrow(() -> new ExchangeRateNotFoundException(foreignCurrency));

        final Order order;
        if (fromCurrency == Currency.KRW) {
            // Case A: KRW → 외화 매수 (buyRate 적용)
            BigDecimal buyRate = latestRate.getBuyRate();
            // JPY는 buyRate가 100엔 기준이므로 1엔 기준으로 변환하여 계산
            BigDecimal effectiveRate = foreignCurrency.isJpy()
                ? ExchangeRateCalculator.toUnitRate(buyRate)
                : buyRate;
            BigDecimal krwAmount = ExchangeRateCalculator.calcKrwFromForex(forexAmount, effectiveRate);
            order = Order.of(krwAmount, Currency.KRW, forexAmount, toCurrency, buyRate);
        } else {
            // Case B: 외화 → KRW 매도 (sellRate 적용)
            BigDecimal sellRate = latestRate.getSellRate();
            // JPY는 sellRate가 100엔 기준이므로 1엔 기준으로 변환하여 계산
            BigDecimal effectiveRate = foreignCurrency.isJpy()
                ? ExchangeRateCalculator.toUnitRate(sellRate)
                : sellRate;
            BigDecimal krwAmount = ExchangeRateCalculator.calcKrwToForex(forexAmount, effectiveRate);
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
