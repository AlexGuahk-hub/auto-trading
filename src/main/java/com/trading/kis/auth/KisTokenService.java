package com.trading.kis.auth;

import com.trading.kis.config.KisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class KisTokenService {

    private final KisProperties props;
    private final WebClient kisWebClient;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_KEY = "kis:access_token";

    public KisTokenService(KisProperties props,
                           @Qualifier("kisWebClient") WebClient kisWebClient,
                           RedisTemplate<String, String> redisTemplate) {
        this.props = props;
        this.kisWebClient = kisWebClient;
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) return cached;

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", props.getAppKey(),
                "appsecret", props.getAppSecret()
        );

        KisTokenResponse resp = kisWebClient.post()
                .uri("/oauth2/tokenP")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .block();

        if (resp == null || resp.getAccessToken() == null) {
            throw new IllegalStateException("KIS 토큰 발급 실패: 응답이 null");
        }

        // 만료 30분 전 갱신을 위해 23시간만 캐싱
        redisTemplate.opsForValue().set(TOKEN_KEY, resp.getAccessToken(), Duration.ofHours(23));
        log.info("KIS 액세스 토큰 발급 완료");
        return resp.getAccessToken();
    }

    public void invalidateToken() {
        redisTemplate.delete(TOKEN_KEY);
        log.info("KIS 토큰 캐시 삭제");
    }
}
