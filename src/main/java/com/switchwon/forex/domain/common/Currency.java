package com.switchwon.forex.domain.common;

/**
 * 지원 통화 열거형
 * - KRW는 주문 기준 통화로만 사용 (환율 조회 대상 아님)
 * - Enum 사용 이유: 허용되지 않은 통화 코드 입력 시 컴파일/런타임 단계에서 조기 차단
 */
public enum Currency {
    USD, JPY, CNY, EUR, KRW;

    /**
     * 해당 통화가 외화(Foreign Currency)인지 여부
     * KRW를 제외한 4개 통화가 환율 조회 및 주문의 외화 대상
     */
    public boolean isForeign() {
        return this != KRW;
    }

    /**
     * JPY 여부 확인
     * JPY는 100엔 단위 환산 특례 적용
     */
    public boolean isJpy() {
        return this == JPY;
    }
}
