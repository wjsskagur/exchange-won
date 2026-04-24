package com.switchwon.forex.presentation.order.dto;

import com.switchwon.forex.domain.common.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 주문 요청 DTO
 *
 * @NotNull + @DecimalMin: 입력값 검증을 컨트롤러 레이어에서 조기 차단
 * → 서비스까지 잘못된 값이 전파되는 것 방지
 *
 * forexAmount는 항상 외화 기준 (과제 명세: "주문의 금액은 외화 기준")
 */
public record OrderRequest(

    @NotNull(message = "외화 금액(forexAmount)은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "외화 금액은 0보다 커야 합니다.")
    BigDecimal forexAmount,

    @NotNull(message = "출금 통화(fromCurrency)는 필수입니다.")
    Currency fromCurrency,

    @NotNull(message = "입금 통화(toCurrency)는 필수입니다.")
    Currency toCurrency
) {
}
