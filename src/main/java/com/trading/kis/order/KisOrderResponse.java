package com.trading.kis.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KisOrderResponse {
    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output")
    private Output output;

    @Data
    public static class Output {
        @JsonProperty("KST_ORD_NO")
        private String kstnOrdno;   // 주문번호

        @JsonProperty("ORD_TMD")
        private String ordTmd;      // 주문시각
    }
}
