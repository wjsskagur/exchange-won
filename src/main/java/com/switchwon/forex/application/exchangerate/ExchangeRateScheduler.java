package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;
import com.switchwon.forex.domain.exchangerate.ExchangeRateRepository;
import com.switchwon.forex.infrastructure.client.ExchangeRateApiClient;
import com.switchwon.forex.infrastructure.client.MockExchangeRateGenerator;
import com.switchwon.forex.infrastructure.client.dto.ExchangeRateApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 환율 정보 수집 스케줄러
 *
 * 동작 전략:
 * 1. 외부 API(open.er-api.com) 호출 → 실제 시세 수집 후 DB 저장
 * 2. API 호출 실패 시 → MockExchangeRateGenerator(현실적 시세 범위의 랜덤값)로 Fallback
 *
 * 랜덤 변동을 항상 적용하지 않는 이유:
 * - 과제 명세: "임의의 랜덤값 추가는 무방합니다" → 허용이지 요구사항이 아님
 * - 외부 API가 정상이면 실제 시세를 그대로 저장하는 것이 올바른 동작
 * - 랜덤 변동은 API 실패 시 Mock Fallback 내부에서만 적용
 *
 * cron("0 * * * * *") 사용 이유:
 * - 매 분 정각(00초)에 실행 → 시각 동기화
 * - fixedRate: API 지연 시 중첩 실행 위험
 * - fixedDelay: 실행 완료 기준이라 정확한 1분 주기 보장 안 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateApiClient apiClient;
    private final MockExchangeRateGenerator mockGenerator;
    private final ExchangeRateRepository exchangeRateRepository;

    @Value("${forex.api.use-mock-fallback:true}")
    private boolean useMockFallback;

    private static final List<Currency> TARGET_CURRENCIES = List.of(
        Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR
    );

    /**
     * 1분마다 환율 수집 (매 분 0초 실행)
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void collectExchangeRates() {
        log.info("환율 수집 스케줄러 시작");

        Map<Currency, BigDecimal> tradeStanRates = fetchTradeStanRates();

        if (tradeStanRates.isEmpty()) {
            log.error("수집된 환율 데이터 없음. DB 저장 건너뜀");
            return;
        }

        saveExchangeRates(tradeStanRates);
        log.info("환율 수집 완료. 저장 통화 수={}", tradeStanRates.size());
    }

    /**
     * 앱 시작 시 즉시 1회 수집 (최초 1분 공백 방지)
     *
     * initialDelay=3000: Spring 컨텍스트 완전 초기화 대기
     * fixedDelay=Long.MAX_VALUE: 이후 재실행 없음 (cron 스케줄러가 담당)
     */
    @Scheduled(initialDelay = 3000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void collectOnStartup() {
        log.info("애플리케이션 시작 - 초기 환율 수집");
        Map<Currency, BigDecimal> tradeStanRates = fetchTradeStanRates();
        if (!tradeStanRates.isEmpty()) {
            saveExchangeRates(tradeStanRates);
        }
    }

    /**
     * 환율 데이터 수집
     * 1순위: 외부 API 실제 데이터
     * 2순위: Mock Fallback (API 실패 시, use-mock-fallback=true인 경우)
     */
    private Map<Currency, BigDecimal> fetchTradeStanRates() {
        ExchangeRateApiResponse apiResponse = apiClient.fetchRatesBasedOnKrw();

        if (apiResponse != null && apiResponse.isSuccess() && apiResponse.rates() != null) {
            log.info("외부 API 환율 수집 성공");
            return extractFromApiResponse(apiResponse);
        }

        if (useMockFallback) {
            log.warn("외부 API 실패 → Mock 데이터로 Fallback");
            return mockGenerator.generateMockRates();
        }

        return Map.of();
    }

    /**
     * API 응답(1 KRW 기준 외화량)을 매매기준율(1외화 기준 KRW)로 변환
     *
     * open.er-api.com /KRW 응답 예시:
     * { "USD": 0.000724, "JPY": 0.10989, "CNY": 0.00507, "EUR": 0.000653 }
     * 의미: 1 KRW = 0.000724 USD → 역수 계산 → 1 USD = 1381.21 KRW
     */
    private Map<Currency, BigDecimal> extractFromApiResponse(ExchangeRateApiResponse response) {
        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);

        for (Currency currency : TARGET_CURRENCIES) {
            BigDecimal apiRate = response.rates().get(currency.name());
            if (apiRate == null) {
                log.warn("API 응답에 {} 환율 없음", currency);
                continue;
            }
            try {
                BigDecimal tradeStanRate = ExchangeRateCalculator.toTradeStanRate(apiRate, currency);
                result.put(currency, tradeStanRate);
                log.debug("환율 변환: {} → 매매기준율 {} KRW", currency, tradeStanRate);
            } catch (Exception e) {
                log.error("환율 변환 실패: {} apiRate={}", currency, apiRate, e);
            }
        }

        return result;
    }

    /**
     * 매매기준율로부터 buy/sell rate 계산 후 DB 일괄 저장
     *
     * saveAll() 사용 이유: 4건 개별 save() 대신 단일 배치 insert로 DB 왕복 최소화
     */
    private void saveExchangeRates(Map<Currency, BigDecimal> tradeStanRates) {
        List<ExchangeRate> entities = new ArrayList<>();

        for (Map.Entry<Currency, BigDecimal> entry : tradeStanRates.entrySet()) {
            Currency currency = entry.getKey();
            BigDecimal tradeStanRate = entry.getValue();

            BigDecimal buyRate = ExchangeRateCalculator.calcBuyRate(tradeStanRate);
            BigDecimal sellRate = ExchangeRateCalculator.calcSellRate(tradeStanRate);

            entities.add(ExchangeRate.of(currency, tradeStanRate, buyRate, sellRate));
        }

        exchangeRateRepository.saveAll(entities);
    }
}
