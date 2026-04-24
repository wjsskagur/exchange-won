package com.switchwon.forex.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;
import com.switchwon.forex.domain.exchangerate.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API 통합 테스트
 *
 * @SpringBootTest + @AutoConfigureMockMvc:
 * - 전체 Spring 컨텍스트 로딩 → 실제 Bean 주입
 * - MockMvc로 HTTP 요청/응답 검증 (실제 서버 기동 불필요)
 * - H2 인메모리 DB 사용으로 테스트 격리
 *
 * @Transactional:
 * - 각 테스트 메서드 종료 후 롤백 → 테스트 간 데이터 오염 방지
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("API 통합 테스트")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 환율 데이터 사전 삽입
        // 스케줄러 실행을 기다리지 않고 테스트 가능하도록
        exchangeRateRepository.save(
            ExchangeRate.of(Currency.USD,
                new BigDecimal("1477.45"),
                new BigDecimal("1551.32"),
                new BigDecimal("1403.58"))
        );
        exchangeRateRepository.save(
            ExchangeRate.of(Currency.JPY,
                new BigDecimal("910.50"),
                new BigDecimal("956.03"),
                new BigDecimal("864.98"))
        );
        exchangeRateRepository.save(
            ExchangeRate.of(Currency.CNY,
                new BigDecimal("190.00"),
                new BigDecimal("199.50"),
                new BigDecimal("180.50"))
        );
        exchangeRateRepository.save(
            ExchangeRate.of(Currency.EUR,
                new BigDecimal("1530.00"),
                new BigDecimal("1606.50"),
                new BigDecimal("1453.50"))
        );
    }

    // ===================== 환율 조회 API =====================

    @Test
    @DisplayName("GET /exchange-rate/latest - 4개 통화 최신 환율 전체 조회")
    void getLatestAll_success() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.returnObject.exchangeRateList").isArray())
            .andExpect(jsonPath("$.returnObject.exchangeRateList", hasSize(4)));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/USD - USD 환율 상세 조회")
    void getLatestByCurrency_usd_success() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.returnObject.currency").value("USD"))
            .andExpect(jsonPath("$.returnObject.buyRate").value(1551.32))
            .andExpect(jsonPath("$.returnObject.tradeStanRate").value(1477.45))
            .andExpect(jsonPath("$.returnObject.sellRate").value(1403.58))
            .andExpect(jsonPath("$.returnObject.dateTime").isNotEmpty());
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/KRW - KRW는 환율 없음, 404 반환")
    void getLatestByCurrency_krw_notFound() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/KRW"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/XXX - 잘못된 통화 코드, 400 반환")
    void getLatestByCurrency_invalidCurrency_badRequest() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/XXX"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ===================== 주문 API =====================

    @Test
    @DisplayName("POST /order - KRW → USD 매수 성공")
    void placeOrder_buyUsd_success() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            Map.of("forexAmount", 200, "fromCurrency", "KRW", "toCurrency", "USD")
        );

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
            .andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
            .andExpect(jsonPath("$.returnObject.toAmount").value(200.0))
            .andExpect(jsonPath("$.returnObject.tradeRate").value(1551.32))
            // KRW 절사 확인: 200 * 1551.32 = 310264.0 -> 310264
            .andExpect(jsonPath("$.returnObject.fromAmount").value(310264))
            // 주문 생성 응답에는 id 없음
            .andExpect(jsonPath("$.returnObject.id").doesNotExist());
    }

    @Test
    @DisplayName("POST /order - USD → KRW 매도 성공")
    void placeOrder_sellUsd_success() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            Map.of("forexAmount", 133, "fromCurrency", "USD", "toCurrency", "KRW")
        );

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.returnObject.fromCurrency").value("USD"))
            .andExpect(jsonPath("$.returnObject.toCurrency").value("KRW"))
            .andExpect(jsonPath("$.returnObject.fromAmount").value(133))
            .andExpect(jsonPath("$.returnObject.tradeRate").value(1403.58))
            // 133 × 1403.58 = 186676.14 → floor = 186676
            .andExpect(jsonPath("$.returnObject.toAmount").value(186676));
    }

    @Test
    @DisplayName("POST /order - forexAmount가 0이면 400 반환")
    void placeOrder_zeroAmount_badRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            Map.of("forexAmount", 0, "fromCurrency", "KRW", "toCurrency", "USD")
        );

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /order - USD → JPY 외화끼리 조합은 400 반환")
    void placeOrder_foreignToForeign_badRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            Map.of("forexAmount", 100, "fromCurrency", "USD", "toCurrency", "JPY")
        );

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ===================== 주문 목록 API =====================

    @Test
    @DisplayName("GET /order/list - 주문 내역 조회 (주문 후)")
    void getOrderList_afterOrder_success() throws Exception {
        // 주문 먼저 실행
        String buyRequest = objectMapper.writeValueAsString(
            Map.of("forexAmount", 200, "fromCurrency", "KRW", "toCurrency", "USD")
        );
        mockMvc.perform(post("/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(buyRequest));

        // 목록 조회
        mockMvc.perform(get("/order/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.returnObject.orderList").isArray())
            .andExpect(jsonPath("$.returnObject.orderList[0].id").isNumber())  // id 존재 확인
            .andExpect(jsonPath("$.returnObject.orderList", hasSize(greaterThanOrEqualTo(1))));
    }
}
