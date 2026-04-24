package com.switchwon.forex.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate을 Bean으로 등록하는 이유:
     * - new RestTemplate()으로 직접 생성하면 스프링의 빈 라이프사이클 밖에 있어
     *   인터셉터, 메트릭 수집, 테스트 시 Mock 주입 등이 어려움
     * - Bean으로 등록하면 테스트에서 MockRestServiceServer로 교체 가능
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
