package com.trading.upbit.watchlist;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_coin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchCoinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market", nullable = false, unique = true, length = 20)
    private String market;

    @Column(name = "coin_name", length = 50)
    private String coinName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** null이면 global default(trading.coin-order-amount-krw) 사용 */
    @Column(name = "order_amount_krw", precision = 20, scale = 2)
    private BigDecimal orderAmountKrw;

    @Column(length = 200)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
