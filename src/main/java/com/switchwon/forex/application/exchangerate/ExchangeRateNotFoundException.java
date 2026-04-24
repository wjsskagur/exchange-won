package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.domain.common.Currency;

/**
 * 환율 데이터 없을 때 발생하는 예외
 *
 * RuntimeException 상속 이유:
 * - Checked Exception은 호출 계층마다 throws 선언이 필요해 코드가 복잡해짐
 * - 애플리케이션 로직 예외는 RuntimeException으로 처리하고
 *   GlobalExceptionHandler에서 일괄 처리하는 것이 Spring 표준 패턴
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(Currency currency) {
        super(String.format("'%s' 통화의 환율 정보가 없습니다. 스케줄러 실행 후 다시 시도해주세요.", currency));
    }
}
