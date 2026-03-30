package com.trading.upbit.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpbitCandleResponse {
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

    @JsonProperty("candle_acc_trade_volume")
    private double candleAccTradeVolume;

    @JsonProperty("timestamp")
    private long timestamp;
}
