package com.trading.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceDto {
    private String market;
    private double currentPrice;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double volume;
}
