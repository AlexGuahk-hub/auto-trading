package com.trading.upbit.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpbitTickerResponse {
    @JsonProperty("market")
    private String market;

    @JsonProperty("trade_price")
    private double tradePrice;

    @JsonProperty("opening_price")
    private double openingPrice;

    @JsonProperty("high_price")
    private double highPrice;

    @JsonProperty("low_price")
    private double lowPrice;

    @JsonProperty("acc_trade_volume_24h")
    private double accTradeVolume;

    @JsonProperty("signed_change_rate")
    private double signedChangeRate;
}
