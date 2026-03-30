package com.trading.kis.order;

import com.trading.common.OrderResult;
import com.trading.kis.auth.KisTokenService;
import com.trading.kis.config.KisProperties;
import com.trading.notification.TelegramNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class KisOrderService {

    private final KisProperties props;
    private final KisTokenService tokenService;
    private final WebClient kisWebClient;
    private final TelegramNotifier notifier;

    public KisOrderService(KisProperties props,
                           KisTokenService tokenService,
                           @Qualifier("kisWebClient") WebClient kisWebClient,
                           TelegramNotifier notifier) {
        this.props = props;
        this.tokenService = tokenService;
        this.kisWebClient = kisWebClient;
        this.notifier = notifier;
    }

    public OrderResult buyMarket(String stockCode, int quantity) {
        return placeOrder(stockCode, quantity, "01", "BUY", "0");
    }

    public OrderResult sellMarket(String stockCode, int quantity) {
        return placeOrder(stockCode, quantity, "01", "SELL", "0");
    }

    public OrderResult buyLimit(String stockCode, int quantity, long price) {
        return placeOrder(stockCode, quantity, "00", "BUY", String.valueOf(price));
    }

    private OrderResult placeOrder(String stockCode, int quantity,
                                   String orderDivision, String side, String price) {
        String trId;
        if (props.isPaper()) {
            trId = "BUY".equals(side) ? "VTTC0802U" : "VTTC0801U";
        } else {
            trId = "BUY".equals(side) ? "TTTC0802U" : "TTTC0801U";
        }

        Map<String, String> body = Map.of(
                "CANO", props.getAccountNo(),
                "ACNT_PRDT_CD", props.getAccountProductCode(),
                "PDNO", stockCode,
                "ORD_DVSN", orderDivision,
                "ORD_QTY", String.valueOf(quantity),
                "ORD_UNPR", price
        );

        final String finalTrId = trId;
        KisOrderResponse resp = kisWebClient.post()
                .uri("/uapi/domestic-stock/v1/trading/order-cash")
                .headers(h -> {
                    h.set("authorization", "Bearer " + tokenService.getAccessToken());
                    h.set("appkey", props.getAppKey());
                    h.set("appsecret", props.getAppSecret());
                    h.set("tr_id", finalTrId);
                    h.set("custtype", "P");
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisOrderResponse.class)
                .block();

        if (resp == null) {
            log.error("[KIS] 주문 응답 null: {} {} {}주", side, stockCode, quantity);
            return OrderResult.builder()
                    .exchange("KIS").market(stockCode).side(side)
                    .quantity(quantity).success(false).build();
        }

        String orderId = (resp.getOutput() != null) ? resp.getOutput().getKstnOrdno() : null;
        OrderResult result = OrderResult.builder()
                .exchange("KIS")
                .market(stockCode)
                .side(side)
                .quantity(quantity)
                .orderId(orderId)
                .success("0".equals(resp.getRtCd()))
                .build();

        notifier.sendOrderNotification(result);
        log.info("[KIS] 주문: {} {} {}주 - orderId: {}", side, stockCode, quantity, orderId);
        return result;
    }
}
