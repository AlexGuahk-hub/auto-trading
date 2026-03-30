package com.trading.upbit.market;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoinPriceDto {
    private String market;
    private double currentPrice;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double volume;
    private double changeRate;
}
