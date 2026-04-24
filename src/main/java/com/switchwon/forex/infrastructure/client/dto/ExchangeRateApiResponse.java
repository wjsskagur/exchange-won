package com.switchwon.forex.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * open.er-api.com API 응답 DTO
 *
 * 예시 응답:
 * {
 *   "result": "success",
 *   "base_code": "KRW",
 *   "rates": {
 *     "USD": 0.000677,
 *     "JPY": 0.10236,
 *     ...
 *   }
 * }
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) 사용 이유:
 * - 외부 API는 우리가 제어할 수 없어 필드가 추가될 수 있음
 * - 필요한 필드만 매핑하고 나머지는 무시해 API 변경에 유연하게 대응
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse(
    String result,
    Map<String, BigDecimal> rates
) {
    public boolean isSuccess() {
        return "success".equals(result);
    }
}
