package com.switchwon.forex.application.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.switchwon.forex.domain.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 결과 DTO
 *
 * @JsonInclude(NON_NULL): id가 null이면 JSON 응답에서 해당 필드 자체를 생략
 * - 주문 생성 응답: id 없음 (API 명세에 id 필드 없음)
 * - 주문 목록 응답: id 포함 (API 명세에 id: pkey 명시)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDto(
    Long id,
    BigDecimal fromAmount,
    String fromCurrency,
    BigDecimal toAmount,
    String toCurrency,
    BigDecimal tradeRate,
    LocalDateTime dateTime
) {
    public static OrderDto from(Order order) {
        return new OrderDto(
            null,
            order.getFromAmount(),
            order.getFromCurrency().name(),
            order.getToAmount(),
            order.getToCurrency().name(),
            order.getTradeRate(),
            order.getCreatedAt()
        );
    }

    public static OrderDto fromWithId(Order order) {
        return new OrderDto(
            order.getId(),
            order.getFromAmount(),
            order.getFromCurrency().name(),
            order.getToAmount(),
            order.getToCurrency().name(),
            order.getTradeRate(),
            order.getCreatedAt()
        );
    }
}
