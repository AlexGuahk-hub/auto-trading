package com.trading.strategy;

import com.trading.common.PriceDto;
import com.trading.common.TradeSignal;
import com.trading.common.TradingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class BollingerBandStrategy implements TradingStrategy {

    private static final int PERIOD = 20;
    private static final double MULTIPLIER = 2.0;

    @Override
    public String getName() {
        return "BollingerBand";
    }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < PERIOD) return TradeSignal.HOLD;

        List<PriceDto> window = prices.subList(prices.size() - PERIOD, prices.size());
        double sma = window.stream().mapToDouble(PriceDto::getCurrentPrice).average().orElse(0);

        double variance = window.stream()
                .mapToDouble(p -> Math.pow(p.getCurrentPrice() - sma, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        double upperBand = sma + MULTIPLIER * stdDev;
        double lowerBand = sma - MULTIPLIER * stdDev;
        double currentPrice = prices.get(prices.size() - 1).getCurrentPrice();

        if (currentPrice <= lowerBand) {
            log.info("[{}] 볼린저 하단 터치 price:{} lower:{}", market, currentPrice, lowerBand);
            return TradeSignal.BUY;
        }
        if (currentPrice >= upperBand) {
            log.info("[{}] 볼린저 상단 터치 price:{} upper:{}", market, currentPrice, upperBand);
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }
}
