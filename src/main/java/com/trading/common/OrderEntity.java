package com.trading.common;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_order")
@Getter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange", nullable = false, length = 10)
    private String exchange;

    @Column(name = "market", nullable = false, length = 20)
    private String market;

    @Column(name = "side", nullable = false, length = 4)
    private String side;

    @Column(name = "quantity", precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "amount_krw", precision = 20, scale = 2)
    private BigDecimal amountKrw;

    @Column(name = "price", precision = 20, scale = 2)
    private BigDecimal price;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "strategy", length = 50)
    private String strategy;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public OrderEntity(String exchange, String market, String side, BigDecimal quantity,
                       BigDecimal amountKrw, BigDecimal price, String orderId,
                       String strategy, String status, LocalDateTime createdAt) {
        this.exchange = exchange;
        this.market = market;
        this.side = side;
        this.quantity = quantity;
        this.amountKrw = amountKrw;
        this.price = price;
        this.orderId = orderId;
        this.strategy = strategy;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OrderEntity from(OrderResult result) {
        return OrderEntity.builder()
                .exchange(result.getExchange())
                .market(result.getMarket())
                .side(result.getSide())
                .quantity(result.getQuantity() > 0
                        ? BigDecimal.valueOf(result.getQuantity()) : null)
                .amountKrw(result.getAmount())
                .orderId(result.getOrderId())
                .strategy(result.getStrategy())
                .status(result.isSuccess() ? "FILLED" : "FAILED")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
