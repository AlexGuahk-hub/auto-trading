package com.trading.notification;

import com.trading.common.OrderEntity;
import com.trading.common.OrderRepository;
import com.trading.common.OrderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TelegramNotifier {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    @Autowired
    private OrderRepository orderRepository;

    private final WebClient telegramClient = WebClient.create("https://api.telegram.org");

    public void sendOrderNotification(OrderResult order) {
        String icon   = "BUY".equals(order.getSide()) ? "매수" : "매도";
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
        sendMessage(buildDailyReport("KIS"));
    }

    // 일일 리포트 — 매일 자정 코인
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void sendCoinDailyReport() {
        sendMessage(buildDailyReport("UPBIT"));
    }

    private String buildDailyReport(String exchange) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);

        List<OrderEntity> orders = orderRepository
                .findByExchangeAndCreatedAtBetween(exchange, startOfDay, endOfDay);

        if (orders.isEmpty()) {
            String label = "KIS".equals(exchange) ? "[주식]" : "[코인]";
            return String.format("%s 오늘 매매 없음 (%s)", label, today);
        }

        List<OrderEntity> filled = orders.stream()
                .filter(o -> "FILLED".equals(o.getStatus())).toList();
        List<OrderEntity> failed = orders.stream()
                .filter(o -> "FAILED".equals(o.getStatus())).toList();

        List<OrderEntity> buys  = filled.stream().filter(o -> "BUY".equals(o.getSide())).toList();
        List<OrderEntity> sells = filled.stream().filter(o -> "SELL".equals(o.getSide())).toList();

        StringBuilder sb = new StringBuilder();

        if ("KIS".equals(exchange)) {
            sb.append("📊 [주식] 오늘 매매 리포트\n");
            sb.append("날짜: ").append(today).append("\n");
            sb.append("──────────────────\n");
            sb.append(formatKisOrders("매수", buys));
            sb.append(formatKisOrders("매도", sells));
        } else {
            sb.append("📊 [코인] 오늘 매매 리포트\n");
            sb.append("날짜: ").append(today).append("\n");
            sb.append("──────────────────\n");
            sb.append(formatUpbitOrders("매수", buys));
            sb.append(formatUpbitOrders("매도", sells));
        }

        if (!failed.isEmpty()) {
            sb.append("실패: ").append(failed.size()).append("건\n");
        }
        sb.append("──────────────────\n");
        sb.append("총 ").append(filled.size()).append("건 체결");

        return sb.toString();
    }

    private String formatKisOrders(String label, List<OrderEntity> orders) {
        if (orders.isEmpty()) return "";

        // 종목별 수량 합산
        Map<String, Integer> byMarket = orders.stream()
                .collect(Collectors.groupingBy(
                        OrderEntity::getMarket,
                        Collectors.summingInt(o -> o.getQuantity() != null
                                ? o.getQuantity().intValue() : 0)));

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" ").append(orders.size()).append("건\n");
        byMarket.forEach((market, qty) ->
                sb.append("  ").append(market).append(" ").append(qty).append("주\n"));
        return sb.toString();
    }

    private String formatUpbitOrders(String label, List<OrderEntity> orders) {
        if (orders.isEmpty()) return "";

        // 종목별 금액/수량 합산
        Map<String, BigDecimal> byMarket = orders.stream()
                .collect(Collectors.groupingBy(
                        OrderEntity::getMarket,
                        Collectors.reducing(BigDecimal.ZERO,
                                o -> o.getAmountKrw() != null ? o.getAmountKrw() : BigDecimal.ZERO,
                                BigDecimal::add)));

        BigDecimal totalAmount = byMarket.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" ").append(orders.size()).append("건");
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" | ").append(String.format("%,.0f", totalAmount)).append("원");
        }
        sb.append("\n");

        byMarket.forEach((market, amount) -> {
            sb.append("  ").append(market);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(" ").append(String.format("%,.0f", amount)).append("원");
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    public void sendErrorAlert(String message) {
        sendMessage("⚠️ 경고: " + message);
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
