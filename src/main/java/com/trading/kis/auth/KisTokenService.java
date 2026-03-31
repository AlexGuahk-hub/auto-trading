package com.trading.kis.auth;

import com.trading.kis.config.KisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class KisTokenService {

    private final KisProperties props;
    private final WebClient kisWebClient;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_KEY = "kis:access_token";

    // Redis 연결 실패 시 폴백용 인메모리 캐시
    private final Map<String, String> memoryCache = new ConcurrentHashMap<>();

    public KisTokenService(KisProperties props,
                           @Qualifier("kisWebClient") WebClient kisWebClient,
                           RedisTemplate<String, String> redisTemplate) {
        this.props = props;
        this.kisWebClient = kisWebClient;
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        String cached = getFromCache();
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

        saveToCache(resp.getAccessToken());
        log.info("KIS 액세스 토큰 발급 완료");
        return resp.getAccessToken();
    }

    public void invalidateToken() {
        try {
            redisTemplate.delete(TOKEN_KEY);
        } catch (Exception e) {
            log.warn("Redis 삭제 실패: {}", e.getMessage());
        }
        memoryCache.remove(TOKEN_KEY);
        log.info("KIS 토큰 캐시 삭제");
    }

    private String getFromCache() {
        try {
            return redisTemplate.opsForValue().get(TOKEN_KEY);
        } catch (Exception e) {
            log.warn("Redis 조회 실패, 메모리 캐시 사용: {}", e.getMessage());
            return memoryCache.get(TOKEN_KEY);
        }
    }

    private void saveToCache(String token) {
        try {
            redisTemplate.opsForValue().set(TOKEN_KEY, token, Duration.ofHours(23));
        } catch (Exception e) {
            log.warn("Redis 저장 실패, 메모리 캐시에 저장: {}", e.getMessage());
            memoryCache.put(TOKEN_KEY, token);
        }
    }
}
