# 실시간 환율 기반 외환 주문 시스템

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Build | Gradle 8.7 (Kotlin DSL) |
| DB | H2 In-Memory |
| ORM | Spring Data JPA (Hibernate 6) |
| Test | JUnit 5, Mockito, MockMvc |

---

## 프로젝트 구조

```
forex-order-system/
├── build.gradle.kts          # Gradle 빌드 설정 (Kotlin DSL)
├── settings.gradle.kts       # 프로젝트 이름 설정
├── gradlew                   # Gradle Wrapper (Unix)
├── gradlew.bat               # Gradle Wrapper (Windows)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar    # ※ 아래 setup 참고
│   └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/switchwon/forex/
    │   │   ├── ForexOrderSystemApplication.java
    │   │   ├── domain/               # 엔티티, Repository 인터페이스
    │   │   ├── application/          # 서비스, 스케줄러, 계산기
    │   │   ├── infrastructure/       # 외부 API 클라이언트
    │   │   └── presentation/         # 컨트롤러, 예외 핸들러
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/switchwon/forex/
            ├── application/exchangerate/ExchangeRateCalculatorTest.java
            ├── application/order/OrderServiceTest.java
            └── presentation/ApiIntegrationTest.java
```

---

## 최초 설정 (gradle-wrapper.jar)

`gradle-wrapper.jar`는 바이너리 파일이라 Git에 포함되지 않습니다.  
아래 방법 중 하나로 설정하세요.

```bash
# 방법 1: Gradle이 이미 설치된 경우
gradle wrapper --gradle-version 8.7

# 방법 2: jar 파일 직접 다운로드
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar
```

---

## 실행 방법

```bash
# 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun

# 실행 가능한 JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/forex-order-system.jar
```

Windows:
```cmd
gradlew.bat bootRun
```

서버 포트: `8080`  
H2 콘솔: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:forexdb` / 사용자: `sa` / 비밀번호: (없음)

---

## API 명세

### 1. 전체 통화 최신 환율 조회
```
GET /exchange-rate/latest
```

```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "buyRate": 1551.32,
        "tradeStanRate": 1477.45,
        "sellRate": 1403.58,
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
`currency`: USD, JPY, CNY, EUR

### 3. 외화 주문

```
POST /order
Content-Type: application/json
```

**Case A: KRW → 외화 매수** (buyRate 적용)
```json
{ "forexAmount": 200, "fromCurrency": "KRW", "toCurrency": "USD" }
```

**Case B: 외화 → KRW 매도** (sellRate 적용)
```json
{ "forexAmount": 133, "fromCurrency": "USD", "toCurrency": "KRW" }
```

### 4. 주문 내역 조회
```
GET /order/list
```

---

## 에러 응답

| 상황 | HTTP | code |
|---|---|---|
| 환율 데이터 없음 | 404 | NOT_FOUND |
| 지원하지 않는 통화 조합 | 400 | BAD_REQUEST |
| 입력값 검증 실패 | 400 | INVALID_INPUT |
| 서버 내부 오류 | 500 | INTERNAL_ERROR |

---

## 주요 설계 결정

### 레이어드 아키텍처
```
presentation  →  application  →  domain
                      ↑
               infrastructure
```
의존 방향이 단방향으로 고정되어 `application`이 `presentation`을 참조하는 역방향 의존이 없습니다.

### 금융 계산 - BigDecimal 전면 사용
`double`: `0.1 + 0.2 = 0.30000000000000004` (오차)  
`BigDecimal`: `0.1 + 0.2 = 0.3` (정확)

| 항목 | 처리 |
|---|---|
| 환율 저장값 | scale=2, HALF_UP |
| KRW 환산금액 | scale=0, FLOOR (원화 절사) |

### 환율 수집 전략
1. `open.er-api.com` API 호출 (API 키 불필요)
2. 실패 시 → Mock 데이터 (현실 시세 ±1% 랜덤)

스케줄러:
- `cron = "0 * * * * *"`: 매 분 정각 수집
- `initialDelay = 3000`: 앱 시작 3초 후 즉시 1회 수집