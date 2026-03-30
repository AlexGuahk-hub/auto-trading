package com.trading.upbit.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinPriceRepository extends JpaRepository<CoinPriceEntity, Long> {

    @Query(value = "SELECT * FROM coin_price WHERE market = :market " +
            "ORDER BY collected_at DESC LIMIT :limit", nativeQuery = true)
    List<CoinPriceEntity> findRecentByMarket(@Param("market") String market,
                                             @Param("limit") int limit);
}
