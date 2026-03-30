package com.trading.common;

import java.util.List;

public interface TradingStrategy {
    String getName();
    TradeSignal analyze(String market, List<PriceDto> prices);
}
