package com.trading.upbit.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpbitOrderResponse {
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("side")
    private String side;

    @JsonProperty("ord_type")
    private String ordType;

    @JsonProperty("price")
    private String price;

    @JsonProperty("state")
    private String state;

    @JsonProperty("market")
    private String market;

    @JsonProperty("created_at")
    private String createdAt;
}
