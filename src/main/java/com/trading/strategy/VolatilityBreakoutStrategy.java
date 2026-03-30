package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// 래리 윌리엄스 변동성 돌파 전략 (k=0.5) — 코인 특화
@Component
@Slf4j
public class VolatilityBreakoutStrategy implements TradingStrategy {

    private static final double K = 0.5;

    @Override
    public String getName() {
        return "VolatilityBreakout";
    }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < 2) return TradeSignal.HOLD;

        PriceDto prev = prices.get(prices.size() - 2);
        PriceDto current = prices.get(prices.size() - 1);

        double range = prev.getHighPrice() - prev.getLowPrice();
        double target = prev.getOpenPrice() + range * K;
        double curPrice = current.getCurrentPrice();

        if (curPrice >= target) {
            log.info("[{}] 변동성 돌파 target:{} current:{}", market, target, curPrice);
            return TradeSignal.BUY;
        }
        return TradeSignal.HOLD;
    }
}
