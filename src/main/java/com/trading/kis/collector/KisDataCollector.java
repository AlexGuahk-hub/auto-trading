package com.trading.kis.collector;

import com.trading.kis.market.KisStockService;
import com.trading.kis.market.StockPriceDto;
import com.trading.kis.market.StockPriceEntity;
import com.trading.kis.market.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class KisDataCollector {

    private final KisStockService stockService;
    private final StockPriceRepository priceRepository;

    @Value("${trading.stocks}")
    private List<String> watchList;

    // 장중 1분마다 시세 수집 (평일 09:00~15:30)
    @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void collectPrices() {
        watchList.forEach(code -> {
            try {
                StockPriceDto price = stockService.getCurrentPrice(code);
                if (price == null) {
                    log.warn("[KIS] 시세 조회 null: {}", code);
                    return;
                }
                priceRepository.save(StockPriceEntity.from(price));
                log.debug("[KIS] 시세 수집: {} - {}원", code, price.getCurrentPrice());
                Thread.sleep(200); // KIS Rate Limit: 초당 5회 제한
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[KIS] 시세 수집 중단: {}", code);
            } catch (Exception e) {
                log.error("[KIS] 시세 수집 실패: {}", code, e);
            }
        });
    }
}
