package com.trading.upbit.scheduler;

import com.trading.common.RiskCheckResult;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStateManager;
import com.trading.notification.TelegramNotifier;
import com.trading.risk.RiskManager;
import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.UpbitMarketService;
import com.trading.upbit.order.UpbitOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitTradingScheduler {

    private final UpbitMarketService marketService;
    private final UpbitOrderService orderService;
    private final RiskManager riskManager;
    private final TradingStateManager stateManager;
    private final TelegramNotifier notifier;

    @Value("${trading.coins}")
    private List<String> coins;

    @Value("${trading.coin-order-amount-krw}")
    private BigDecimal orderAmountKrw;

    // 5분마다 전략 실행 (24시간 365일 — 코인은 시간 제한 없음)
    @Scheduled(fixedDelay = 300_000)
    public void runStrategy() {
        if (!stateManager.isUpbitEnabled()) {
            log.debug("[업비트] 자동매매 중지 상태 — 실행 건너뜀");
            return;
        }
        log.info("[업비트] 자동매매 전략 실행");
        coins.forEach(market -> {
            try {
                analyzeAndTrade(market);
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[업비트] 전략 실행 중단: {}", market);
            } catch (Exception e) {
                log.error("[업비트] 전략 실행 오류: {}", market, e);
                notifier.sendErrorAlert(String.format("[업비트] 전략 실행 오류 [%s]\n원인: %s", market, e.getMessage()));
            }
        });
    }

    private void analyzeAndTrade(String market) {
        // 1시간봉 48개 조회 (2일치)
        List<CoinPriceDto> candles = marketService.getMinuteCandles(market, 60, 48);
        if (candles.size() < 20) return;

        // 변동성 돌파 전략 (코인에 특화)
        TradeSignal signal = volatilityBreakout(market, candles);
        if (signal == TradeSignal.HOLD) return;

        double currentPrice = candles.get(candles.size() - 1).getCurrentPrice();
        RiskCheckResult risk = riskManager.checkCoinOrder(market, orderAmountKrw, (long) currentPrice);
        if (!risk.isApproved()) {
            log.warn("[업비트] 리스크 거부 [{}]: {}", market, risk.getReason());
            return;
        }

        if (signal == TradeSignal.BUY) {
            orderService.buyMarket(market, orderAmountKrw);
        } else {
            BigDecimal holding = marketService.getHoldingVolume(market);
            if (holding.compareTo(BigDecimal.ZERO) > 0) {
                orderService.sellMarket(market, holding);
            }
        }
    }

    // 래리 윌리엄스 변동성 돌파 전략 (k=0.5)
    private TradeSignal volatilityBreakout(String market, List<CoinPriceDto> candles) {
        CoinPriceDto prev = candles.get(candles.size() - 2);
        CoinPriceDto current = candles.get(candles.size() - 1);

        double range = prev.getHighPrice() - prev.getLowPrice();
        double target = prev.getOpenPrice() + range * 0.5;
        double curPrice = current.getCurrentPrice();

        if (curPrice >= target) {
            log.info("[업비트] 변동성 돌파 [{}] target:{} current:{}", market, target, curPrice);
            return TradeSignal.BUY;
        }
        return TradeSignal.HOLD;
    }

}
