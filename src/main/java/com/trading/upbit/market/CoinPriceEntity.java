package com.trading.upbit.market;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_price")
@Getter
@NoArgsConstructor
public class CoinPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market", nullable = false, length = 20)
    private String market;

    @Column(name = "current_price", nullable = false, precision = 20, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "open_price", precision = 20, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 20, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 20, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "volume", precision = 30, scale = 8)
    private BigDecimal volume;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public CoinPriceEntity(String market, BigDecimal currentPrice, BigDecimal openPrice,
                            BigDecimal highPrice, BigDecimal lowPrice, BigDecimal volume,
                            LocalDateTime collectedAt) {
        this.market = market;
        this.currentPrice = currentPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.collectedAt = collectedAt;
    }

    public static CoinPriceEntity from(CoinPriceDto dto) {
        return CoinPriceEntity.builder()
                .market(dto.getMarket())
                .currentPrice(BigDecimal.valueOf(dto.getCurrentPrice()))
                .openPrice(BigDecimal.valueOf(dto.getOpenPrice()))
                .highPrice(BigDecimal.valueOf(dto.getHighPrice()))
                .lowPrice(BigDecimal.valueOf(dto.getLowPrice()))
                .volume(BigDecimal.valueOf(dto.getVolume()))
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
