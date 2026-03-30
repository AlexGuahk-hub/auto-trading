package com.trading.upbit.market;

import com.trading.upbit.auth.UpbitJwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class UpbitMarketService {

    private final WebClient upbitWebClient;
    private final UpbitJwtProvider jwtProvider;

    public UpbitMarketService(@Qualifier("upbitWebClient") WebClient upbitWebClient,
                              UpbitJwtProvider jwtProvider) {
        this.upbitWebClient = upbitWebClient;
        this.jwtProvider = jwtProvider;
    }

    // 현재가 조회 (인증 불필요 — Public API)
    public CoinPriceDto getCurrentPrice(String market) {
        return upbitWebClient.get()
                .uri(u -> u.path("/v1/ticker")
                        .queryParam("markets", market)
                        .build())
                .retrieve()
                .bodyToFlux(UpbitTickerResponse.class)
                .map(r -> CoinPriceDto.builder()
                        .market(market)
                        .currentPrice(r.getTradePrice())
                        .openPrice(r.getOpeningPrice())
                        .highPrice(r.getHighPrice())
                        .lowPrice(r.getLowPrice())
                        .volume(r.getAccTradeVolume())
                        .changeRate(r.getSignedChangeRate())
                        .build())
                .blockFirst();
    }

    // 분봉 캔들 조회 (최대 200개)
    public List<CoinPriceDto> getMinuteCandles(String market, int unit, int count) {
        List<CoinPriceDto> result = upbitWebClient.get()
                .uri(u -> u.path("/v1/candles/minutes/" + unit)
                        .queryParam("market", market)
                        .queryParam("count", Math.min(count, 200))
                        .build())
                .retrieve()
                .bodyToFlux(UpbitCandleResponse.class)
                .map(r -> CoinPriceDto.builder()
                        .market(market)
                        .currentPrice(r.getTradePrice())
                        .openPrice(r.getOpeningPrice())
                        .highPrice(r.getHighPrice())
                        .lowPrice(r.getLowPrice())
                        .volume(r.getCandleAccTradeVolume())
                        .build())
                .collectList()
                .block();

        if (result == null) return Collections.emptyList();
        Collections.reverse(result); // 오름차순 정렬
        return result;
    }

    // KRW 보유 잔고 조회
    public BigDecimal getKrwBalance() {
        return getBalances().stream()
                .filter(b -> "KRW".equals(b.getCurrency()))
                .map(b -> new BigDecimal(b.getBalance()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    // 특정 코인 보유 수량 조회
    public BigDecimal getHoldingVolume(String market) {
        String currency = market.replace("KRW-", "");
        return getBalances().stream()
                .filter(b -> currency.equals(b.getCurrency()))
                .map(b -> new BigDecimal(b.getBalance()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    // 전체 잔고 조회 (인증 필요) — null 대신 빈 리스트 반환
    public List<UpbitBalanceDto> getBalances() {
        List<UpbitBalanceDto> result = upbitWebClient.get()
                .uri("/v1/accounts")
                .header("Authorization", "Bearer " + jwtProvider.createToken())
                .retrieve()
                .bodyToFlux(UpbitBalanceDto.class)
                .collectList()
                .block();
        return result != null ? result : Collections.emptyList();
    }
}
