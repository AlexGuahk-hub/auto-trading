package com.trading.kis.market;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_price")
@Getter
@NoArgsConstructor
public class StockPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(name = "open_price")
    private Long openPrice;

    @Column(name = "high_price")
    private Long highPrice;

    @Column(name = "low_price")
    private Long lowPrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "change_rate", precision = 6, scale = 2)
    private BigDecimal changeRate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public StockPriceEntity(String stockCode, Long currentPrice, Long openPrice,
                             Long highPrice, Long lowPrice, Long volume,
                             BigDecimal changeRate, LocalDateTime collectedAt) {
        this.stockCode = stockCode;
        this.currentPrice = currentPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.changeRate = changeRate;
        this.collectedAt = collectedAt;
    }

    public static StockPriceEntity from(StockPriceDto dto) {
        return StockPriceEntity.builder()
                .stockCode(dto.getCode())
                .currentPrice(dto.getCurrentPrice())
                .openPrice(dto.getOpenPrice())
                .highPrice(dto.getHighPrice())
                .lowPrice(dto.getLowPrice())
                .volume(dto.getVolume())
                .changeRate(dto.getChangeRate())
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
