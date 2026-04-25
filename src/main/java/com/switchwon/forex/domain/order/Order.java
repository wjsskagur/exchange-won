package com.switchwon.forex.domain.order;

import com.switchwon.forex.domain.common.Currency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 외화 주문 내역 엔티티
 *
 * 설계 결정:
 * - fromAmount/toAmount를 BigDecimal로 통일:
 *   KRW는 정수지만 외화는 소수점 발생 가능 (e.g., 200.0 USD)
 *   응답 스펙에서 200.0처럼 소수점 표현이 필요하므로 BigDecimal 유지
 * - tradeRate: 주문 시점 적용 환율을 스냅샷으로 저장
 *   환율은 계속 변하므로, 주문 당시 어떤 환율이 적용됐는지 기록 보존 필수
 */
@Entity
@Table(name = "forex_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 출금 금액 (KRW→USD면 KRW 금액, USD→KRW면 USD 금액)
     */
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal fromAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency fromCurrency;

    /**
     * 입금 금액 (주문 결과로 받는 금액)
     */
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal toAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency toCurrency;

    /**
     * 주문 시점에 적용된 환율 스냅샷
     * buyRate(매수 시) 또는 sellRate(매도 시)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tradeRate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static Order of(BigDecimal fromAmount, Currency fromCurrency,
                            BigDecimal toAmount, Currency toCurrency,
                            BigDecimal tradeRate) {
        Order order = new Order();
        order.fromAmount = fromAmount;
        order.fromCurrency = fromCurrency;
        order.toAmount = toAmount;
        order.toCurrency = toCurrency;
        order.tradeRate = tradeRate;
        order.createdAt = LocalDateTime.now().withNano(0);
        return order;
    }
}
