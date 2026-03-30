package com.trading.common;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderResult {
    private String exchange;   // KIS / UPBIT
    private String market;     // 005930 / KRW-BTC
    private String side;       // BUY / SELL
    private int quantity;
    private BigDecimal amount;
    private String orderId;
    private boolean success;
    private String strategy;
}
