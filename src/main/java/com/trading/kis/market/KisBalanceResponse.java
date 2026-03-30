package com.trading.kis.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KisBalanceResponse {
    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("output1")
    private List<Output1> output1;

    @JsonProperty("output2")
    private List<Output2> output2;

    @Data
    public static class Output1 {
        @JsonProperty("pdno")
        private String pdno;          // 종목코드

        @JsonProperty("prdt_name")
        private String prdtName;      // 종목명

        @JsonProperty("hldg_qty")
        private String hldgQty;       // 보유수량

        @JsonProperty("pchs_avg_pric")
        private String pchsAvgPric;   // 매입평균가

        @JsonProperty("prpr")
        private String prpr;          // 현재가

        @JsonProperty("evlu_pfls_rt")
        private String evluPflsRt;    // 평가손익율
    }

    @Data
    public static class Output2 {
        @JsonProperty("tot_evlu_amt")
        private String totEvluAmt;    // 총평가금액

        @JsonProperty("nass_amt")
        private String nassAmt;       // 순자산금액

        @JsonProperty("prvs_rcdl_excc_amt")
        private String prvsRcdlExccAmt; // 가용현금
    }
}
