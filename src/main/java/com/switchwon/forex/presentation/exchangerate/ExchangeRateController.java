package com.switchwon.forex.presentation.exchangerate;

import com.switchwon.forex.application.exchangerate.ExchangeRateService;
import com.switchwon.forex.application.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * GET /exchange-rate/latest
     * 전체 통화 최신 환율 조회
     * returnObject: { "exchangeRateList": [...] }
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Map<String, List<ExchangeRateDto>>>> getLatestAll() {
        List<ExchangeRateDto> list = exchangeRateService.getLatestAll();
        return ResponseEntity.ok(ApiResponse.success(Map.of("exchangeRateList", list)));
    }

    /**
     * GET /exchange-rate/latest/{currency}
     * 특정 통화 최신 환율 조회
     * Spring이 PathVariable 문자열을 Currency enum으로 자동 변환
     * 잘못된 값 입력 시 MethodArgumentTypeMismatchException -> GlobalExceptionHandler 처리
     */
    @GetMapping("/latest/{currency}")
    public ResponseEntity<ApiResponse<ExchangeRateDto>> getLatestByCurrency(
            @PathVariable Currency currency) {
        ExchangeRateDto result = exchangeRateService.getLatestByCurrency(currency);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
