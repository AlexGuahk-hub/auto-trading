package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 단기 급등 모멘텀 전략 (단타)
 *
 * BUY  : 5봉 수익률 >= 1.5% AND 현재 거래량 >= 10봉 평균 거래량
 * SELL : 3MA < 7MA (단기 데드크로스 — 모멘텀 소멸 즉시 이탈)
 */
@Component
@Slf4j
public class MomentumSurgeStrategy implements TradingStrategy {

    private static final int    ROC_PERIOD      = 5;
    private static final double ROC_THRESHOLD   = 0.015;  // 1.5%
    private static final int    VOLUME_LOOKBACK = 10;
    private static final int    SHORT_MA        = 3;
    private static final int    LONG_MA         = 7;
    private static final int    MIN_DATA        = VOLUME_LOOKBACK + ROC_PERIOD + 1;

    @Override
    public String getName() { return "MomentumSurge"; }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < MIN_DATA) return TradeSignal.HOLD;

        PriceDto current  = prices.get(prices.size() - 1);
        PriceDto pastRoc  = prices.get(prices.size() - 1 - ROC_PERIOD);

        double roc = (current.getCurrentPrice() - pastRoc.getCurrentPrice())
                / pastRoc.getCurrentPrice();

        double avgVolume = prices.stream()
                .skip(prices.size() - VOLUME_LOOKBACK - 1)
                .limit(VOLUME_LOOKBACK)
                .mapToDouble(PriceDto::getVolume)
                .average().orElse(0);

        if (roc >= ROC_THRESHOLD && current.getVolume() >= avgVolume) {
            log.info("[{}][MomentumSurge] 5봉 급등 {:.2f}% + 거래량 확인 → 매수",
                    market, roc * 100);
            return TradeSignal.BUY;
        }

        double ma3 = avg(prices, SHORT_MA);
        double ma7 = avg(prices, LONG_MA);
        double prevMa3 = avg(prices.subList(0, prices.size() - 1), SHORT_MA);
        double prevMa7 = avg(prices.subList(0, prices.size() - 1), LONG_MA);

        // 단기 데드크로스 (3MA가 7MA 아래로 교차)
        if (prevMa3 >= prevMa7 && ma3 < ma7) {
            log.info("[{}][MomentumSurge] 3MA({}) < 7MA({}) 데드크로스 → 매도", market, ma3, ma7);
            return TradeSignal.SELL;
        }

        return TradeSignal.HOLD;
    }

    private double avg(List<PriceDto> prices, int period) {
        return prices.stream()
                .skip(Math.max(0, prices.size() - period))
                .mapToDouble(PriceDto::getCurrentPrice)
                .average().orElse(0);
    }
}
