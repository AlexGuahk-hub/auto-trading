package com.trading.upbit.collector;

import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.UpbitMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitDataCollector {

    private final UpbitMarketService marketService;

    @Value("${trading.coins}")
    private List<String> coins;

    // 5분마다 시세 수집 (24시간 365일)
    @Scheduled(fixedDelay = 300_000)
    public void collectPrices() {
        coins.forEach(market -> {
            try {
                CoinPriceDto price = marketService.getCurrentPrice(market);
                if (price == null) {
                    log.warn("[업비트] 시세 조회 null: {}", market);
                    return;
                }
                log.debug("[업비트] 시세 수집: {} - {}원", market, price.getCurrentPrice());
                Thread.sleep(150); // 업비트 Rate Limit 준수
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[업비트] 시세 수집 중단: {}", market);
            } catch (Exception e) {
                log.error("[업비트] 시세 수집 실패: {}", market, e);
            }
        });
    }
}
