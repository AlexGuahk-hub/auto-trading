package com.trading.upbit.order;

import com.trading.common.OrderResult;
import com.trading.notification.TelegramNotifier;
import com.trading.upbit.auth.UpbitJwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
@Slf4j
public class UpbitOrderService {

    private final WebClient upbitWebClient;
    private final UpbitJwtProvider jwtProvider;
    private final TelegramNotifier notifier;

    public UpbitOrderService(@Qualifier("upbitWebClient") WebClient upbitWebClient,
                             UpbitJwtProvider jwtProvider,
                             TelegramNotifier notifier) {
        this.upbitWebClient = upbitWebClient;
        this.jwtProvider = jwtProvider;
        this.notifier = notifier;
    }

    // 시장가 매수 (원화 금액 기준) — ord_type: price
    public OrderResult buyMarket(String market, BigDecimal krwAmount) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market", market);
        params.add("side", "bid");
        params.add("price", krwAmount.toPlainString());
        params.add("ord_type", "price");
        return placeOrder(market, "BUY", params);
    }

    // 시장가 매도 (코인 수량 기준) — ord_type: market
    public OrderResult sellMarket(String market, BigDecimal volume) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market", market);
        params.add("side", "ask");
        params.add("volume", volume.toPlainString());
        params.add("ord_type", "market");
        return placeOrder(market, "SELL", params);
    }

    // 지정가 매수
    public OrderResult buyLimit(String market, BigDecimal volume, BigDecimal price) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market", market);
        params.add("side", "bid");
        params.add("volume", volume.toPlainString());
        params.add("price", price.toPlainString());
        params.add("ord_type", "limit");
        return placeOrder(market, "BUY", params);
    }

    private OrderResult placeOrder(String market, String side,
                                   MultiValueMap<String, String> params) {
        // 파라미터 기반 JWT 생성 — 업비트는 매 요청마다 새로 생성 (캐싱 불가)
        String jwt = jwtProvider.createToken(params);

        UpbitOrderResponse resp = upbitWebClient.post()
                .uri("/v1/orders")
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(params.toSingleValueMap())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        r.bodyToMono(String.class)
                                .map(body -> new RuntimeException("[업비트] 주문 오류: " + body)))
                .bodyToMono(UpbitOrderResponse.class)
                .block();

        if (resp == null) {
            log.error("[업비트] 주문 응답 null: {} {}", side, market);
            return OrderResult.builder()
                    .exchange("UPBIT").market(market).side(side).success(false).build();
        }

        OrderResult result = OrderResult.builder()
                .exchange("UPBIT")
                .market(market)
                .side(side)
                .orderId(resp.getUuid())
                .success(resp.getUuid() != null)
                .build();

        notifier.sendOrderNotification(result);
        log.info("[업비트] 주문: {} {} - uuid: {}", side, market, resp.getUuid());
        return result;
    }
}
