package com.switchwon.forex.application.exchangerate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 단건 응답 DTO
 *
 * @JsonFormat 사용 이유:
 * - Jackson의 write-dates-as-timestamps=false 설정만으로는
 *   LocalDateTime의 나노초까지 직렬화됨 (e.g. "2026-04-25T12:20:58.8005488")
 * - @JsonFormat으로 포맷을 명시하면 초 단위까지만 출력 보장
 *   (e.g. "2026-04-25T12:20:58")
 */
public record ExchangeRateDto(
    String currency,
    BigDecimal buyRate,
    BigDecimal tradeStanRate,
    BigDecimal sellRate,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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
