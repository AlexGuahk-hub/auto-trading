package com.trading.kis.market;

import com.trading.kis.auth.KisTokenService;
import com.trading.kis.config.KisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KisStockService {

    private final KisProperties props;
    private final KisTokenService tokenService;
    private final WebClient kisWebClient;

    public KisStockService(KisProperties props,
                           KisTokenService tokenService,
                           @Qualifier("kisWebClient") WebClient kisWebClient) {
        this.props = props;
        this.tokenService = tokenService;
        this.kisWebClient = kisWebClient;
    }

    public StockPriceDto getCurrentPrice(String stockCode) {
        KisStockPriceResponse resp = kisWebClient.get()
                .uri(u -> u.path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .build())
                .headers(this::setCommonHeaders)
                .header("tr_id", "FHKST01010100")
                .retrieve()
                .bodyToMono(KisStockPriceResponse.class)
                .block();

        if (resp == null || resp.getOutput() == null) {
            log.warn("[KIS] 현재가 조회 응답 null: {}", stockCode);
            return null;
        }

        KisStockPriceResponse.Output o = resp.getOutput();
        return StockPriceDto.builder()
                .code(stockCode)
                .currentPrice(parseLongSafe(o.getStckPrpr()))
                .openPrice(parseLongSafe(o.getStckOprc()))
                .highPrice(parseLongSafe(o.getStckHgpr()))
                .lowPrice(parseLongSafe(o.getStckLwpr()))
                .volume(parseLongSafe(o.getAcmlVol()))
                .changeRate(o.getPrdyCtrt() != null ? new BigDecimal(o.getPrdyCtrt()) : BigDecimal.ZERO)
                .build();
    }

    public AccountBalanceDto getAccountBalance() {
        String trId = props.isPaper() ? "VTTC8434R" : "TTTC8434R";

        KisBalanceResponse resp = kisWebClient.get()
                .uri(u -> u.path("/uapi/domestic-stock/v1/trading/inquire-balance")
                        .queryParam("CANO", props.getAccountNo())
                        .queryParam("ACNT_PRDT_CD", props.getAccountProductCode())
                        .queryParam("AFHR_FLPR_YN", "N")
                        .queryParam("OFL_YN", "N")
                        .queryParam("INQR_DVSN", "02")
                        .queryParam("UNPR_DVSN", "01")
                        .queryParam("FUND_STTL_ICLD_YN", "N")
                        .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                        .queryParam("PRCS_DVSN", "00")
                        .queryParam("CTX_AREA_FK100", "")
                        .queryParam("CTX_AREA_NK100", "")
                        .build())
                .headers(this::setCommonHeaders)
                .header("tr_id", trId)
                .retrieve()
                .bodyToMono(KisBalanceResponse.class)
                .block();

        if (resp == null) {
            log.warn("[KIS] 잔고 조회 응답 null");
            return AccountBalanceDto.builder()
                    .totalAsset(0).availableCash(0)
                    .holdings(Collections.emptyList()).build();
        }
        return mapToBalanceDto(resp);
    }

    private AccountBalanceDto mapToBalanceDto(KisBalanceResponse resp) {
        List<AccountBalanceDto.HoldingDto> holdings = Collections.emptyList();
        if (resp.getOutput1() != null) {
            holdings = resp.getOutput1().stream()
                    .filter(o -> parseLongSafe(o.getHldgQty()) > 0)
                    .map(o -> AccountBalanceDto.HoldingDto.builder()
                            .stockCode(o.getPdno())
                            .stockName(o.getPrdtName())
                            .quantity((int) parseLongSafe(o.getHldgQty()))
                            .avgPrice(parseLongSafe(o.getPchsAvgPric().replace(".", "")))
                            .currentPrice(parseLongSafe(o.getPrpr()))
                            .profitRate(parseDoubleSafe(o.getEvluPflsRt()))
                            .build())
                    .collect(Collectors.toList());
        }

        long totalAsset = 0;
        long availableCash = 0;
        if (resp.getOutput2() != null && !resp.getOutput2().isEmpty()) {
            KisBalanceResponse.Output2 out2 = resp.getOutput2().get(0);
            totalAsset = parseLongSafe(out2.getTotEvluAmt());
            availableCash = parseLongSafe(out2.getPrvsRcdlExccAmt());
        } else {
            log.warn("[KIS] 잔고 output2 비어있음");
        }

        return AccountBalanceDto.builder()
                .totalAsset(totalAsset)
                .availableCash(availableCash)
                .holdings(holdings)
                .build();
    }

    private void setCommonHeaders(HttpHeaders headers) {
        headers.set("authorization", "Bearer " + tokenService.getAccessToken());
        headers.set("appkey", props.getAppKey());
        headers.set("appsecret", props.getAppSecret());
    }

    private long parseLongSafe(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
