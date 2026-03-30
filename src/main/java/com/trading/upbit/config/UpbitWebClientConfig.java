package com.trading.upbit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class UpbitWebClientConfig {

    private final UpbitProperties props;

    @Bean("upbitWebClient")
    public WebClient upbitWebClient() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // Remaining-Req 헤더 모니터링 필터
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    String remaining = resp.headers().asHttpHeaders().getFirst("Remaining-Req");
                    if (remaining != null) log.debug("[업비트] Rate Limit: {}", remaining);
                    return Mono.just(resp);
                }))
                .build();
    }
}
