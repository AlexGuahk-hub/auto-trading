package com.trading.notification;

import com.trading.common.OrderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class TelegramNotifier {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    private final WebClient telegramClient = WebClient.create("https://api.telegram.org");

    public void sendOrderNotification(OrderResult order) {
        String icon = "BUY".equals(order.getSide()) ? "매수" : "매도";
        String prefix = "KIS".equals(order.getExchange()) ? "[주식]" : "[코인]";
        String amountStr = "KIS".equals(order.getExchange())
                ? order.getQuantity() + "주"
                : (order.getAmount() != null ? order.getAmount() + "원" : "");

        sendMessage(String.format("%s %s %s 체결\n종목: %s\n주문ID: %s",
                prefix, icon, amountStr,
                order.getMarket(), order.getOrderId()));
    }

    // 일일 리포트 — 평일 16:00 주식
    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void sendStockDailyReport() {
        sendMessage("[주식] 오늘 매매 리포트\n수익: 집계 중...");
    }

    // 일일 리포트 — 매일 자정 코인
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void sendCoinDailyReport() {
        sendMessage("[코인] 오늘 매매 리포트\n수익: 집계 중...");
    }

    public void sendErrorAlert(String message) {
        sendMessage("경고: " + message);
    }

    public void sendMessage(String text) {
        telegramClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .bodyValue(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(null, e -> log.error("텔레그램 전송 실패: {}", e.getMessage()));
    }
}
