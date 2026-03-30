package com.trading.kis.market;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AccountBalanceDto {
    private long totalAsset;
    private long availableCash;
    private List<HoldingDto> holdings;

    @Data
    @Builder
    public static class HoldingDto {
        private String stockCode;
        private String stockName;
        private int quantity;
        private long avgPrice;
        private long currentPrice;
        private double profitRate;
    }
}
