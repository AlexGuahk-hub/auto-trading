package com.trading.kis.market;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockPriceDto {
    private String code;
    private long currentPrice;
    private long openPrice;
    private long highPrice;
    private long lowPrice;
    private long volume;
    private BigDecimal changeRate;
}
