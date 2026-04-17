package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 거래량 급증 + 신고가 돌파 전략 (단타)
 *
 * BUY  : 현재 거래량 >= 직전 10봉 평균의 2.5배 AND 현재가 >= 직전 5봉 최고가 (돌파)
 * SELL : 현재가 < 5이동평균 (추세 약화 즉시 이탈)
 */
@Component
@Slf4j
public class VolumeBreakoutStrategy implements TradingStrategy {

    private static final int    VOLUME_LOOKBACK = 10;
    private static final double VOLUME_RATIO    = 2.5;
    private static final int    HIGH_LOOKBACK   = 5;
    private static final int    SELL_MA         = 5;
    private static final int    MIN_DATA        = VOLUME_LOOKBACK + HIGH_LOOKBACK + 1;

    @Override
    public String getName() { return "VolumeBreakout"; }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < MIN_DATA) return TradeSignal.HOLD;

        PriceDto current = prices.get(prices.size() - 1);

        double avgVolume = prices.stream()
                .skip(prices.size() - VOLUME_LOOKBACK - 1)
                .limit(VOLUME_LOOKBACK)
                .mapToDouble(PriceDto::getVolume)
                .average().orElse(1);

        double recentHigh = prices.stream()
                .skip(prices.size() - HIGH_LOOKBACK - 1)
                .limit(HIGH_LOOKBACK)
                .mapToDouble(PriceDto::getHighPrice)
                .max().orElse(Double.MAX_VALUE);

        boolean volumeSurge   = avgVolume > 0 && current.getVolume() >= avgVolume * VOLUME_RATIO;
        boolean priceBreakout = current.getCurrentPrice() >= recentHigh;

        if (volumeSurge && priceBreakout) {
            log.info("[{}][VolumeBreakout] 거래량 {:.1f}배 급증 + 신고가({}) 돌파 → 매수",
                    market, current.getVolume() / avgVolume, (long) current.getCurrentPrice());
            return TradeSignal.BUY;
        }

        double ma5 = prices.stream()
                .skip(prices.size() - SELL_MA)
                .mapToDouble(PriceDto::getCurrentPrice)
                .average().orElse(current.getCurrentPrice());

        if (current.getCurrentPrice() < ma5) {
            return TradeSignal.SELL;
        }

        return TradeSignal.HOLD;
    }
}
