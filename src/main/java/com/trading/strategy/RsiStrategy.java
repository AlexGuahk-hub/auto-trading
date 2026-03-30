package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RsiStrategy implements TradingStrategy {

    private static final int PERIOD = 14;
    private static final double OVERSOLD = 30.0;
    private static final double OVERBOUGHT = 70.0;

    @Override
    public String getName() {
        return "RSI";
    }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        double rsi = calcRsi(prices, PERIOD);
        log.debug("[{}] RSI: {}", market, rsi);
        if (rsi < OVERSOLD) return TradeSignal.BUY;
        if (rsi > OVERBOUGHT) return TradeSignal.SELL;
        return TradeSignal.HOLD;
    }

    private double calcRsi(List<PriceDto> prices, int period) {
        if (prices.size() < period + 1) return 50.0;
        double gain = 0, loss = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double diff = prices.get(i).getCurrentPrice() - prices.get(i - 1).getCurrentPrice();
            if (diff > 0) gain += diff;
            else loss -= diff;
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0) return 100.0;
        return 100 - (100 / (1 + avgGain / avgLoss));
    }
}
