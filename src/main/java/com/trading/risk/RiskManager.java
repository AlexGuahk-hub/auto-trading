package com.trading.risk;

import com.trading.common.RiskCheckResult;
import com.trading.kis.market.AccountBalanceDto;
import com.trading.kis.market.KisStockService;
import com.trading.kis.order.KisOrderService;
import com.trading.notification.TelegramNotifier;
import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.UpbitBalanceDto;
import com.trading.upbit.market.UpbitMarketService;
import com.trading.upbit.order.UpbitOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class RiskManager {

    @Value("${trading.stop-loss-ratio}")
    private double stopLossRatio;

    @Value("${trading.daily-loss-limit}")
    private double dailyLossLimit;

    @Value("${trading.max-position-ratio}")
    private double maxPositionRatio;

    private final KisStockService kisService;
    private final UpbitMarketService upbitService;
    private final TelegramNotifier notifier;
    // @Lazy로 순환 의존성 방지
    private final KisOrderService kisOrderService;
    private final UpbitOrderService upbitOrderService;

    public RiskManager(KisStockService kisService,
                       UpbitMarketService upbitService,
                       TelegramNotifier notifier,
                       @Lazy KisOrderService kisOrderService,
                       @Lazy UpbitOrderService upbitOrderService) {
        this.kisService = kisService;
        this.upbitService = upbitService;
        this.notifier = notifier;
        this.kisOrderService = kisOrderService;
        this.upbitOrderService = upbitOrderService;
    }

    // 주식 주문 가능 여부 검사
    public RiskCheckResult checkOrder(String stockCode, int quantity, long price) {
        long orderAmount = price * quantity;

        try {
            AccountBalanceDto balance = kisService.getAccountBalance();
            if (balance.getTotalAsset() > 0) {
                double positionRatio = (double) orderAmount / balance.getTotalAsset();
                if (positionRatio > maxPositionRatio) {
                    return RiskCheckResult.reject(
                            String.format("포지션 한도 초과 %.1f%%", positionRatio * 100));
                }
            }
        } catch (Exception e) {
            log.warn("[리스크] 잔고 조회 실패, 주문 허용: {}", e.getMessage());
        }

        if (isDailyLossExceeded("KIS")) {
            return RiskCheckResult.reject("KIS 일일 손실 한도 초과");
        }

        return RiskCheckResult.approve();
    }

    // 코인 주문 가능 여부 검사
    public RiskCheckResult checkCoinOrder(String market, BigDecimal krwAmount, long currentPrice) {
        try {
            BigDecimal krwBalance = upbitService.getKrwBalance();
            if (krwAmount.compareTo(krwBalance) > 0) {
                return RiskCheckResult.reject("KRW 잔고 부족");
            }
        } catch (Exception e) {
            log.warn("[리스크] 업비트 잔고 조회 실패, 주문 거부: {}", e.getMessage());
            return RiskCheckResult.reject("잔고 조회 실패");
        }

        if (isDailyLossExceeded("UPBIT")) {
            return RiskCheckResult.reject("업비트 일일 손실 한도 초과");
        }

        return RiskCheckResult.approve();
    }

    // 주식 손절 감지 — 5분마다 체크 (장중만)
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void checkKisStopLoss() {
        try {
            AccountBalanceDto balance = kisService.getAccountBalance();
            if (balance == null || balance.getHoldings() == null) return;
            balance.getHoldings().forEach(h -> {
                if (h.getAvgPrice() == 0) return;
                double lossRatio = (double) (h.getCurrentPrice() - h.getAvgPrice()) / h.getAvgPrice();
                if (lossRatio <= -stopLossRatio) {
                    // SLF4J는 {:.1f} 미지원 — String.format으로 포매팅
                    String msg = String.format("[KIS 손절] %s 손실률 %.1f%%", h.getStockCode(), lossRatio * 100);
                    log.warn(msg);
                    notifier.sendErrorAlert(msg);
                    kisOrderService.sellMarket(h.getStockCode(), h.getQuantity());
                }
            });
        } catch (Exception e) {
            log.error("[리스크] KIS 손절 체크 오류", e);
        }
    }

    // 코인 손절 감지 — 5분마다 체크 (24시간)
    @Scheduled(fixedDelay = 300_000)
    public void checkUpbitStopLoss() {
        try {
            upbitService.getBalances().stream()
                    .filter(b -> !"KRW".equals(b.getCurrency())
                            && parseDoubleSafe(b.getBalance()) > 0)
                    .forEach(b -> {
                        String market = "KRW-" + b.getCurrency();
                        CoinPriceDto current = upbitService.getCurrentPrice(market);
                        if (current == null) return;

                        double avgPrice = parseDoubleSafe(b.getAvgBuyPrice());
                        if (Math.abs(avgPrice) < 0.0001) return; // floating-point 0 비교

                        double lossRatio = (current.getCurrentPrice() - avgPrice) / avgPrice;
                        if (lossRatio <= -stopLossRatio) {
                            String msg = String.format("[업비트 손절] %s 손실률 %.1f%%", market, lossRatio * 100);
                            log.warn(msg);
                            notifier.sendErrorAlert(msg);
                            upbitOrderService.sellMarket(market, new BigDecimal(b.getBalance()));
                        }
                    });
        } catch (Exception e) {
            log.error("[리스크] 업비트 손절 체크 오류", e);
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private boolean isDailyLossExceeded(String exchange) {
        // TODO: 오늘 실현 손실 합산 로직 (DB에서 trade_order 집계)
        return false;
    }
}
