package com.switchwon.forex.application.exchangerate;

import com.switchwon.forex.application.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.domain.common.Currency;
import com.switchwon.forex.domain.exchangerate.ExchangeRate;
import com.switchwon.forex.domain.exchangerate.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 환율 조회 서비스
 *
 * @Transactional(readOnly = true):
 * - Hibernate flush/dirty-checking 비활성화 → 성능 향상
 * - 실수로 엔티티 수정해도 DB 반영 안 됨 → 안전성
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public List<ExchangeRateDto> getLatestAll() {
        return exchangeRateRepository.findLatestForAllCurrencies()
            .stream()
            .map(ExchangeRateDto::from)
            .toList();
    }

    public ExchangeRateDto getLatestByCurrency(Currency currency) {
        ExchangeRate rate = exchangeRateRepository
            .findTop1ByCurrencyOrderByCreatedAtDesc(currency)
            .orElseThrow(() -> new ExchangeRateNotFoundException(currency));
        return ExchangeRateDto.from(rate);
    }
}
