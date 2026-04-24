package com.switchwon.forex.presentation.common;

import com.switchwon.forex.application.exchangerate.ExchangeRateNotFoundException;
import com.switchwon.forex.application.order.InvalidOrderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 핸들러
 *
 * @RestControllerAdvice 사용 이유:
 * - 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리
 * - 각 서비스/컨트롤러에 try-catch 분산 방지
 * - 일관된 에러 응답 포맷 보장
 *
 * 예외 계층별 처리:
 * 1. 도메인 예외 (비즈니스 로직 오류) → 4xx
 * 2. 입력값 검증 실패 → 400
 * 3. 예상치 못한 예외 → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 환율 데이터 없음 (스케줄러 미실행 등)
     */
    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleExchangeRateNotFound(ExchangeRateNotFoundException e) {
        log.warn("환율 데이터 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    /**
     * 잘못된 주문 파라미터 (지원하지 않는 통화 조합 등)
     */
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrder(InvalidOrderException e) {
        log.warn("유효하지 않은 주문: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    }

    /**
     * @Valid 검증 실패 (forexAmount <= 0 등)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("입력값이 올바르지 않습니다.");

        log.warn("입력값 검증 실패: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_INPUT", message));
    }

    /**
     * 잘못된 Enum 값 (e.g., currency=XXX)
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(Exception e) {
        log.warn("요청 형식 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_INPUT", "지원하지 않는 통화 코드이거나 요청 형식이 잘못되었습니다. (지원: USD, JPY, CNY, EUR, KRW)"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("정적 리소스 없음: {}", e.getResourcePath()); // DEBUG 레벨로 조용히 처리
        return ResponseEntity.notFound().build();
    }

    /**
     * 예상치 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
