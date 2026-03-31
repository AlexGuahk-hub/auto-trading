package com.trading.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * local 프로필: 스케줄러 스레드 풀을 0으로 설정해 자동매매 스케줄 실행 방지
 */
@Configuration
@Profile("local")
@Slf4j
public class LocalSchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(0);
        scheduler.setThreadNamePrefix("disabled-scheduler-");
        scheduler.setRejectedExecutionHandler((r, e) ->
            log.debug("[로컬] 스케줄 작업 비활성화됨: {}", r.getClass().getSimpleName()));
        scheduler.initialize();
        log.info("[로컬] 스케줄러 비활성화 — API 테스트 전용 모드");
        return scheduler;
    }
}
