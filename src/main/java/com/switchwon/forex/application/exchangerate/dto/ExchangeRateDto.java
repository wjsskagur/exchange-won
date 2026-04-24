package com.switchwon.forex.application.exchangerate.dto;

import com.switchwon.forex.domain.exchangerate.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 단건 응답 DTO
 *
 * DTO 위치를 application 레이어에 두는 이유:
 * - Service가 presentation DTO를 import하면 의존 방향이 역전됨
 *   (application → presentation 불가, presentation → application 이어야 함)
 * - application.dto는 서비스의 출력값을 정의하며,
 *   presentation은 이를 가져다 ApiResponse로 감싸기만 함
 */
public record ExchangeRateDto(
    String currency,
    BigDecimal buyRate,
    BigDecimal tradeStanRate,
    BigDecimal sellRate,
    LocalDateTime dateTime
) {
    public static ExchangeRateDto from(ExchangeRate entity) {
        return new ExchangeRateDto(
            entity.getCurrency().name(),
            entity.getBuyRate(),
            entity.getTradeStanRate(),
            entity.getSellRate(),
            entity.getCreatedAt()
        );
    }
}
