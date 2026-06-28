package com.trading.upbit.collector;

import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.CoinPriceEntity;
import com.trading.upbit.market.CoinPriceRepository;
import com.trading.upbit.market.UpbitMarketService;
import com.trading.upbit.watchlist.WatchCoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitDataCollector {

    private final UpbitMarketService marketService;
    private final CoinPriceRepository priceRepository;
    private final WatchCoinService watchCoinService;

    // 5분마다 시세 수집 (24시간 365일)
    @Scheduled(fixedDelay = 300_000)
    public void collectPrices() {
        watchCoinService.getActiveMarkets().forEach(market -> {
            try {
                CoinPriceDto price = marketService.getCurrentPrice(market);
                if (price == null) {
                    log.warn("[업비트] 시세 조회 null: {}", market);
                    return;
                }
                priceRepository.save(CoinPriceEntity.from(price));
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
