package com.trading.kis.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchStockRepository extends JpaRepository<WatchStockEntity, Long> {
    List<WatchStockEntity> findAllByOrderByCreatedAtAsc();
    List<WatchStockEntity> findByEnabledTrueOrderByCreatedAtAsc();
    Optional<WatchStockEntity> findByStockCode(String stockCode);
    boolean existsByStockCode(String stockCode);
}
