package com.switchwon.forex.domain.exchangerate;

import com.switchwon.forex.domain.common.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * 특정 통화의 최신 환율 1건 조회
     *
     * findTop1By... 방식 사용 이유:
     * - Spring Data가 LIMIT 1 쿼리로 변환 (DB 독립적, 안전)
     * - JPQL LIMIT 절은 Hibernate 버전에 따라 지원 여부가 다름
     * - 인덱스(currency, created_at DESC) 활용으로 성능 충분
     */
    Optional<ExchangeRate> findTop1ByCurrencyOrderByCreatedAtDesc(Currency currency);

    /**
     * 4개 통화 각각의 최신 환율을 한 번에 조회
     *
     * Native Query + GROUP BY MAX 방식 사용 이유:
     * - "각 그룹별 최신 1건" 패턴은 JPQL로 표현이 복잡함
     * - MAX(created_at)로 각 통화의 최신 수집 시각을 구한 뒤 JOIN
     * - H2에서 동일하게 동작하는 표준 SQL 패턴
     */
    @Query(value = """
        SELECT er.*
        FROM exchange_rate_history er
        INNER JOIN (
            SELECT currency, MAX(created_at) AS max_created_at
            FROM exchange_rate_history
            GROUP BY currency
        ) latest
          ON er.currency = latest.currency
         AND er.created_at = latest.max_created_at
        """, nativeQuery = true)
    List<ExchangeRate> findLatestForAllCurrencies();
}
