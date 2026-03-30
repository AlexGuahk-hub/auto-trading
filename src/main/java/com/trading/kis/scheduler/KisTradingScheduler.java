package com.trading.kis.scheduler;

import com.trading.common.PriceDto;
import com.trading.common.RiskCheckResult;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import com.trading.kis.market.KisStockService;
import com.trading.kis.market.StockPriceEntity;
import com.trading.kis.market.StockPriceRepository;
import com.trading.kis.order.KisOrderService;
import com.trading.risk.RiskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class KisTradingScheduler {

    private final KisStockService stockService;
    private final KisOrderService orderService;
    private final RiskManager riskManager;
    private final StockPriceRepository priceRepository;
    private final List<TradingStrategy> strategies;

    @Value("${trading.stocks}")
    private List<String> watchList;

    @Value("${trading.stock-order-quantity}")
    private int orderQuantity;

    // 장중 5분마다 전략 실행 (09:05~15:20, 마감 10분 전 종료)
    @Scheduled(cron = "0 5-20/5 9-14,15 * * MON-FRI", zone = "Asia/Seoul")
    public void runStrategy() {
        log.info("[KIS] 자동매매 전략 실행");
        watchList.forEach(code -> {
            try {
                analyzeAndTrade(code);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[KIS] 전략 실행 중단: {}", code);
            } catch (Exception e) {
                log.error("[KIS] 전략 실행 오류: {}", code, e);
            }
        });
    }

    private void analyzeAndTrade(String stockCode) {
        // DB에서 최근 60개 시세 조회 (전략 계산에 필요한 충분한 데이터)
        List<StockPriceEntity> recent = priceRepository.findRecentByCode(stockCode, 60);
        if (recent.size() < 20) {
            log.debug("[KIS] 데이터 부족 [{}]: {}개 (최소 20개 필요)", stockCode, recent.size());
            return;
        }

        // 시간순 정렬 (findRecentByCode는 DESC 반환) → 전략은 오름차순 필요
        List<PriceDto> prices = recent.stream()
                .sorted((a, b) -> a.getCollectedAt().compareTo(b.getCollectedAt()))
                .map(e -> PriceDto.builder()
                        .market(stockCode)
                        .currentPrice(e.getCurrentPrice())
                        .openPrice(e.getOpenPrice() != null ? e.getOpenPrice() : 0L)
                        .highPrice(e.getHighPrice() != null ? e.getHighPrice() : 0L)
                        .lowPrice(e.getLowPrice() != null ? e.getLowPrice() : 0L)
                        .volume(e.getVolume() != null ? e.getVolume() : 0L)
                        .build())
                .toList();

        long currentPrice = (long) prices.get(prices.size() - 1).getCurrentPrice();

        for (TradingStrategy strategy : strategies) {
            TradeSignal signal = strategy.analyze(stockCode, prices);
            if (signal == TradeSignal.HOLD) continue;

            RiskCheckResult risk = riskManager.checkOrder(stockCode, orderQuantity, currentPrice);
            if (!risk.isApproved()) {
                log.warn("[KIS] 리스크 거부 [{}]: {}", stockCode, risk.getReason());
                continue;
            }

            if (signal == TradeSignal.BUY) {
                orderService.buyMarket(stockCode, orderQuantity);
            } else {
                orderService.sellMarket(stockCode, orderQuantity);
            }
        }
    }
}
