package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MovingAverageStrategy implements TradingStrategy {

    private static final int SHORT = 5;
    private static final int LONG = 20;

    @Override
    public String getName() {
        return "MovingAverage";
    }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < LONG) return TradeSignal.HOLD;

        double shortMa = avg(prices, SHORT);
        double longMa = avg(prices, LONG);
        double prevShortMa = avg(prices.subList(0, prices.size() - 1), SHORT);
        double prevLongMa = avg(prices.subList(0, prices.size() - 1), LONG);

        if (prevShortMa <= prevLongMa && shortMa > longMa) {
            log.info("[{}] 골든크로스 5MA:{} > 20MA:{}", market, shortMa, longMa);
            return TradeSignal.BUY;
        }
        if (prevShortMa >= prevLongMa && shortMa < longMa) {
            log.info("[{}] 데드크로스 5MA:{} < 20MA:{}", market, shortMa, longMa);
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }

    private double avg(List<PriceDto> prices, int period) {
        return prices.stream()
                .skip(Math.max(0, prices.size() - period))
                .mapToDouble(PriceDto::getCurrentPrice)
                .average()
                .orElse(0);
    }
}
