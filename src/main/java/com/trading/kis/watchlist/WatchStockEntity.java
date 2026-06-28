package com.trading.kis.watchlist;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, unique = true, length = 10)
    private String stockCode;

    @Column(name = "stock_name", length = 50)
    private String stockName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** null이면 global default(trading.stock-order-quantity) 사용 */
    @Column(name = "order_qty")
    private Integer orderQty;

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
