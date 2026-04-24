package com.switchwon.forex.domain.exchangerate;

import com.switchwon.forex.domain.common.Currency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 수신 이력 엔티티
 *
 * 설계 결정:
 * - BigDecimal 사용: double/float는 부동소수점 오차로 금융 계산에 부적합.
 *   BigDecimal은 정확한 십진수 연산을 보장하므로 환율/금액에 필수
 * - @NoArgsConstructor(PROTECTED): JPA 스펙상 기본 생성자 필요하나,
 *   외부에서 직접 생성 방지를 위해 protected로 제한 (팩토리 메서드 패턴 유도)
 */
@Entity
@Table(name = "exchange_rate_history",
    indexes = {
        // 최신 환율 조회 쿼리 최적화: currency + createdAt DESC 복합 인덱스
        @Index(name = "idx_currency_created_at", columnList = "currency, created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  // 숫자(ORDINAL) 대신 문자열 저장: Enum 순서 변경 시 데이터 깨짐 방지
    @Column(nullable = false, length = 10)
    private Currency currency;

    /**
     * 매매기준율 (기준이 되는 중간값)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tradeStanRate;

    /**
     * 전신환 매입율 (은행이 외화를 사는 가격 = 고객이 외화를 살 때 적용)
     * = tradeStanRate × 1.05
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal buyRate;

    /**
     * 전신환 매도율 (은행이 외화를 파는 가격 = 고객이 외화를 팔 때 적용)
     * = tradeStanRate × 0.95
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellRate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 팩토리 메서드: 엔티티 생성 의도를 명확히 하고 필수값 강제
     */
    public static ExchangeRate of(Currency currency, BigDecimal tradeStanRate,
                                   BigDecimal buyRate, BigDecimal sellRate) {
        ExchangeRate rate = new ExchangeRate();
        rate.currency = currency;
        rate.tradeStanRate = tradeStanRate;
        rate.buyRate = buyRate;
        rate.sellRate = sellRate;
        rate.createdAt = LocalDateTime.now();
        return rate;
    }
}
