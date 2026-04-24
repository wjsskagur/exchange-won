package com.switchwon.forex.infrastructure.client;

import com.switchwon.forex.infrastructure.client.dto.ExchangeRateApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 환율 API 호출 클라이언트
 *
 * open.er-api.com 사용 이유:
 * - API 키 불필요 (무료, 오픈 접근)
 * - KRW 기준 환율 직접 제공
 * - 응답 구조 단순 (rates 맵에 통화코드:환율)
 *
 * RestTemplate 사용 이유:
 * - 과제 수준의 단순 동기 HTTP 호출에는 WebClient보다 직관적
 * - Spring Boot 3.x에서도 여전히 지원됨
 * - 비동기/리액티브가 필요한 경우 WebClient로 교체 권장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

    private final RestTemplate restTemplate;

    @Value("${forex.api.base-url}")
    private String baseUrl;

    /**
     * KRW 기준 환율 조회
     * 응답값은 "1 KRW = X 외화" 형태이므로, 역수 계산이 필요함
     * (아래 ExchangeRateCalculator에서 처리)
     *
     * @return API 응답 또는 null (호출 실패 시)
     */
    public ExchangeRateApiResponse fetchRatesBasedOnKrw() {
        try {
            // KRW 기준으로 조회: /v6/latest/KRW
            String url = baseUrl + "/KRW";
            ExchangeRateApiResponse response = restTemplate.getForObject(url, ExchangeRateApiResponse.class);

            if (response == null || !response.isSuccess()) {
                log.warn("외부 환율 API 응답 실패 또는 null. url={}", url);
                return null;
            }

            log.debug("외부 환율 API 호출 성공. 통화 수={}", response.rates() != null ? response.rates().size() : 0);
            return response;

        } catch (Exception e) {
            log.error("외부 환율 API 호출 중 예외 발생: {}", e.getMessage());
            return null;
        }
    }
}
