package com.trading.kis.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KisStockPriceResponse {
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
        @JsonProperty("stck_prpr")
        private String stckPrpr;   // 현재가

        @JsonProperty("stck_oprc")
        private String stckOprc;   // 시가

        @JsonProperty("stck_hgpr")
        private String stckHgpr;   // 고가

        @JsonProperty("stck_lwpr")
        private String stckLwpr;   // 저가

        @JsonProperty("acml_vol")
        private String acmlVol;    // 누적 거래량

        @JsonProperty("prdy_ctrt")
        private String prdyCtrt;   // 전일 대비율
    }
}
