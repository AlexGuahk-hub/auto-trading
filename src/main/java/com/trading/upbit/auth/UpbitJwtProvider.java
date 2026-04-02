package com.trading.upbit.auth;

import com.trading.upbit.config.UpbitProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// 업비트는 KIS와 달리 매 요청마다 JWT를 새로 생성해야 함 (캐싱 불가)
@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitJwtProvider {

    private final UpbitProperties props;

    // 파라미터 없는 요청 (잔고 조회 등)
    public String createToken() {
        return buildJwt(null);
    }

    // 파라미터 있는 요청 (주문 등) — 쿼리 해시 필수
    public String createToken(MultiValueMap<String, String> params) {
        String queryString = params.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + "=" + v))
                .collect(Collectors.joining("&"));
        return buildJwt(sha512(queryString));
    }

    private String buildJwt(String queryHash) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("access_key", props.getAccessKey());
        claims.put("nonce", UUID.randomUUID().toString());
        if (queryHash != null) {
            claims.put("query_hash", queryHash);
            claims.put("query_hash_alg", "SHA512");
        }
        return Jwts.builder()
                .claims(claims)
                .signWith(Keys.hmacShaKeyFor(props.getSecretKey().getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 unavailable", e);
        }
    }
}
