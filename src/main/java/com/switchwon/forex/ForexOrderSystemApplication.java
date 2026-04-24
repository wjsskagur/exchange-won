package com.switchwon.forex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // @Scheduled 어노테이션 활성화 - 별도 설정 없이 스케줄러 동작
public class ForexOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForexOrderSystemApplication.class, args);
    }
}
