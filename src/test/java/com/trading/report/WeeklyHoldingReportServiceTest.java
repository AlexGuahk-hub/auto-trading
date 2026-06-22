package com.trading.report;

import com.trading.kis.market.AccountBalanceDto;
import com.trading.upbit.market.UpbitBalanceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyHoldingReportServiceTest {

    private WeeklyHoldingReportService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyHoldingReportService(null, null, null);
    }

    // ── 주식 섹션 ──────────────────────────────────────────

    @Test
    void buildStockSection_보유종목있을때_종목정보를_포함한다() {
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .totalAsset(5_000_000L)
                .availableCash(1_000_000L)
                .holdings(List.of(
                        AccountBalanceDto.HoldingDto.builder()
                                .stockCode("005930")
                                .stockName("삼성전자")
                                .quantity(10)
                                .avgPrice(70_000L)
                                .currentPrice(75_000L)
                                .profitRate(7.14)
                                .build()
                ))
                .build();

        String result = service.buildStockSection(balance);

        assertThat(result).contains("삼성전자");
        assertThat(result).contains("005930");
        assertThat(result).contains("10주");
        assertThat(result).contains("75,000");
        assertThat(result).contains("+7.14%");
        assertThat(result).contains("5,000,000");
    }

    @Test
    void buildStockSection_보유종목없을때_보유없음_메시지를_포함한다() {
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .totalAsset(1_000_000L)
                .availableCash(1_000_000L)
                .holdings(List.of())
                .build();

        String result = service.buildStockSection(balance);

        assertThat(result).contains("보유 주식 없음");
    }

    @Test
    void buildStockSection_손실종목은_마이너스_수익률을_표시한다() {
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .totalAsset(900_000L)
                .availableCash(0L)
                .holdings(List.of(
                        AccountBalanceDto.HoldingDto.builder()
                                .stockCode("000660")
                                .stockName("SK하이닉스")
                                .quantity(1)
                                .avgPrice(200_000L)
                                .currentPrice(180_000L)
                                .profitRate(-10.0)
                                .build()
                ))
                .build();

        String result = service.buildStockSection(balance);

        assertThat(result).contains("-10.00%");
    }

    // ── 코인 섹션 ──────────────────────────────────────────

    @Test
    void buildCoinSection_보유코인있을때_코인정보를_포함한다() {
        List<UpbitBalanceDto> accounts = List.of(
                makeBalance("KRW", "500000", "0"),
                makeBalance("BTC", "0.001", "90000000")
        );

        String result = service.buildCoinSection(accounts);

        assertThat(result).contains("BTC");
        assertThat(result).contains("0.001");
        assertThat(result).contains("500,000");
    }

    @Test
    void buildCoinSection_KRW만있을때_보유코인없음_메시지를_포함한다() {
        List<UpbitBalanceDto> accounts = List.of(
                makeBalance("KRW", "500000", "0")
        );

        String result = service.buildCoinSection(accounts);

        assertThat(result).contains("보유 코인 없음");
        assertThat(result).contains("500,000");
    }

    @Test
    void buildCoinSection_잔고가_0인_코인은_표시하지_않는다() {
        List<UpbitBalanceDto> accounts = List.of(
                makeBalance("KRW", "1000000", "0"),
                makeBalance("ETH", "0.00000001", "0"),  // 먼지 수량
                makeBalance("BTC", "0.005", "95000000")
        );

        String result = service.buildCoinSection(accounts);

        assertThat(result).contains("BTC");
        assertThat(result).doesNotContain("ETH");
    }

    // ── 헬퍼 ──────────────────────────────────────────────

    private UpbitBalanceDto makeBalance(String currency, String balance, String avgBuyPrice) {
        UpbitBalanceDto dto = new UpbitBalanceDto();
        dto.setCurrency(currency);
        dto.setBalance(balance);
        dto.setLocked("0");
        dto.setAvgBuyPrice(avgBuyPrice);
        dto.setUnitCurrency("KRW");
        return dto;
    }
}
