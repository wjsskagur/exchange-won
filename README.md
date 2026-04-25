# 실시간 환율 기반 외환 주문 시스템

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Build | Gradle 8.7 (Groovy DSL) |
| Database | H2 In-Memory DB |
| ORM | Spring Data JPA (Hibernate 6) |
| External API | open.er-api.com (API 키 불필요, 무료) |

---

## 실행 방법

```bash
# 빌드 및 테스트
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 직접 실행
./gradlew bootJar
java -jar build/libs/forex-order-system.jar
```

- 서버 포트: `8080`
- H2 콘솔: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:forexdb`
  - Username: `sa` / Password: (없음)

---

## 프로젝트 구조

```
src/main/java/com/switchwon/forex/
├── domain/                     # 엔티티, Repository 인터페이스
│   ├── common/Currency.java
│   ├── exchangerate/
│   │   ├── ExchangeRate.java
│   │   └── ExchangeRateRepository.java
│   └── order/
│       ├── Order.java
│       └── OrderRepository.java
├── application/                # 비즈니스 로직
│   ├── exchangerate/
│   │   ├── ExchangeRateCalculator.java
│   │   ├── ExchangeRateScheduler.java
│   │   ├── ExchangeRateService.java
│   │   └── ExchangeRateNotFoundException.java
│   └── order/
│       ├── OrderService.java
│       └── InvalidOrderException.java
├── infrastructure/             # 외부 API 연동
│   ├── client/
│   │   ├── ExchangeRateApiClient.java
│   │   └── MockExchangeRateGenerator.java
│   └── config/RestTemplateConfig.java
└── presentation/               # 컨트롤러, 예외 핸들러
    ├── common/
    │   ├── ApiResponse.java
    │   └── GlobalExceptionHandler.java
    ├── exchangerate/ExchangeRateController.java
    └── order/OrderController.java
```

---

## 환율 수집 방식

### 외부 API
`open.er-api.com` 을 사용합니다. API 키 없이 무료로 사용 가능합니다.

```
GET https://open.er-api.com/v6/latest/KRW
응답: { "USD": 0.000724, "JPY": 0.10989, "CNY": 0.00507, "EUR": 0.000653 }
```

응답값은 `1 KRW = X 외화` 형태이므로 역수 계산을 통해 `1 외화 = Y KRW` 매매기준율로 변환합니다.

### 스케줄링
- 앱 시작 3초 후 즉시 1회 수집 (첫 1분 공백 방지)
- 이후 매 분 정각(`cron = "0 * * * * *"`)에 수집 및 DB 저장

### Fallback
네트워크 오류 등 외부 API 호출 실패 시, 현실적인 시세 범위(±1%) 내의 Mock 데이터로 대체합니다.  
`application.yml`의 `forex.api.use-mock-fallback` 값으로 제어합니다.

---

## 환율 계산 규칙

| 항목 | 규칙 |
|---|---|
| 환율 정밀도 | 소수점 둘째 자리 반올림 (HALF_UP) |
| JPY 단위 | 100엔 기준으로 환산 |
| 원화 금액 | 소수점 이하 버림 (FLOOR) |
| 전신환 매입율 | 매매기준율 × 1.05 |
| 전신환 매도율 | 매매기준율 × 0.95 |

---

## API 명세

### 1. 전체 통화 최신 환율 조회

```
GET /exchange-rate/latest
```

```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "buyRate": 1480.43,
        "tradeStanRate": 1409.93,
        "sellRate": 1339.43,
        "dateTime": "2026-04-22T10:01:00"
      }
    ]
  }
}
```

### 2. 특정 통화 최신 환율 조회

```
GET /exchange-rate/latest/{currency}
```

지원 통화: `USD`, `JPY`, `CNY`, `EUR`

### 3. 외화 주문

```
POST /order
Content-Type: application/json
```

**Case A: KRW → 외화 매수** (buyRate 적용)
```json
{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}
```

**Case B: 외화 → KRW 매도** (sellRate 적용)
```json
{
  "forexAmount": 133,
  "fromCurrency": "USD",
  "toCurrency": "KRW"
}
```

### 4. 주문 내역 조회

```
GET /order/list
```

---

## 에러 응답

```json
{
  "code": "에러코드",
  "message": "에러 메시지"
}
```

| 상황 | HTTP Status | code |
|---|---|---|
| 환율 데이터 없음 | 404 | `NOT_FOUND` |
| 지원하지 않는 통화 조합 | 400 | `BAD_REQUEST` |
| 잘못된 요청값 | 400 | `INVALID_INPUT` |
| 서버 내부 오류 | 500 | `INTERNAL_ERROR` |
