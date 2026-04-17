package com.trading.report;

import com.trading.common.OrderEntity;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record ReportOrderDto(
        Long id,
        String exchange,
        String market,
        String side,
        BigDecimal quantity,
        BigDecimal amountKrw,
        BigDecimal price,
        String orderId,
        String strategy,
        String status,
        String createdAt
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ReportOrderDto from(OrderEntity e) {
        return new ReportOrderDto(
                e.getId(),
                e.getExchange(),
                e.getMarket(),
                e.getSide(),
                e.getQuantity(),
                e.getAmountKrw(),
                e.getPrice(),
                e.getOrderId(),
                e.getStrategy(),
                e.getStatus(),
                e.getCreatedAt().format(FMT)
        );
    }
}
