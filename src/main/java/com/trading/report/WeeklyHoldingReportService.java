package com.trading.report;

import com.trading.kis.market.AccountBalanceDto;
import com.trading.kis.market.KisStockService;
import com.trading.notification.TelegramNotifier;
import com.trading.upbit.market.UpbitBalanceDto;
import com.trading.upbit.market.UpbitMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyHoldingReportService {

    private static final BigDecimal DUST_THRESHOLD = new BigDecimal("0.0001");

    private final KisStockService kisStockService;
    private final UpbitMarketService upbitMarketService;
    private final TelegramNotifier notifier;

    @Scheduled(cron = "0 0 10 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyReport() {
        log.info("[주간리포트] 보유 현황 리포트 전송 시작");
        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", java.util.Locale.KOREAN));
            StringBuilder sb = new StringBuilder();
            sb.append("📋 주간 보유 현황 리포트\n");
            sb.append(date).append("\n\n");

            AccountBalanceDto kisBalance = kisStockService.getAccountBalance();
            sb.append(buildStockSection(kisBalance)).append("\n");

            List<UpbitBalanceDto> upbitAccounts = upbitMarketService.getBalances();
            sb.append(buildCoinSection(upbitAccounts));

            notifier.sendMessage(sb.toString());
            log.info("[주간리포트] 전송 완료");
        } catch (Exception e) {
            log.error("[주간리포트] 전송 실패: {}", e.getMessage(), e);
            notifier.sendErrorAlert("[주간리포트] 보유 현황 조회 실패\n원인: " + e.getMessage());
        }
    }

    String buildStockSection(AccountBalanceDto balance) {
        StringBuilder sb = new StringBuilder();
        sb.append("📈 [주식 계좌 - KIS]\n");
        sb.append(String.format("총 자산: %,d원\n", balance.getTotalAsset()));
        sb.append(String.format("예수금: %,d원\n", balance.getAvailableCash()));
        sb.append("──────────────────\n");

        List<AccountBalanceDto.HoldingDto> holdings = balance.getHoldings();
        if (holdings == null || holdings.isEmpty()) {
            sb.append("보유 주식 없음\n");
            return sb.toString();
        }

        for (AccountBalanceDto.HoldingDto h : holdings) {
            String profitSign = h.getProfitRate() >= 0 ? "+" : "";
            sb.append(String.format("종목: %s (%s)\n", h.getStockName(), h.getStockCode()));
            sb.append(String.format("  보유: %d주 | 평균: %,d원 | 현재: %,d원 | 수익률: %s%.2f%%\n",
                    h.getQuantity(), h.getAvgPrice(), h.getCurrentPrice(),
                    profitSign, h.getProfitRate()));
        }
        return sb.toString();
    }

    String buildCoinSection(List<UpbitBalanceDto> accounts) {
        StringBuilder sb = new StringBuilder();
        sb.append("🪙 [코인 계좌 - 업비트]\n");

        BigDecimal krw = accounts.stream()
                .filter(b -> "KRW".equals(b.getCurrency()))
                .map(b -> new BigDecimal(b.getBalance()))
                .findFirst()
                .orElse(BigDecimal.ZERO);

        sb.append(String.format("KRW 잔고: %,.0f원\n", krw));
        sb.append("──────────────────\n");

        List<UpbitBalanceDto> coins = accounts.stream()
                .filter(b -> !"KRW".equals(b.getCurrency()))
                .filter(b -> new BigDecimal(b.getBalance()).compareTo(DUST_THRESHOLD) > 0)
                .toList();

        if (coins.isEmpty()) {
            sb.append("보유 코인 없음\n");
            return sb.toString();
        }

        for (UpbitBalanceDto b : coins) {
            BigDecimal avgPrice = new BigDecimal(b.getAvgBuyPrice());
            sb.append(String.format("종목: %s\n", b.getCurrency()));
            sb.append(String.format("  보유: %s개 | 평균매수: %,.0f원\n",
                    new BigDecimal(b.getBalance()).stripTrailingZeros().toPlainString(),
                    avgPrice));
        }
        return sb.toString();
    }
}
