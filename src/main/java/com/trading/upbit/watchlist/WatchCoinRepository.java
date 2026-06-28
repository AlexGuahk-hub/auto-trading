package com.trading.upbit.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchCoinRepository extends JpaRepository<WatchCoinEntity, Long> {
    List<WatchCoinEntity> findAllByOrderByCreatedAtAsc();
    List<WatchCoinEntity> findByEnabledTrueOrderByCreatedAtAsc();
    Optional<WatchCoinEntity> findByMarket(String market);
    boolean existsByMarket(String market);
}
