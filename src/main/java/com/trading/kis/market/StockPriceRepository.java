package com.trading.kis.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPriceEntity, Long> {

    @Query(value = "SELECT * FROM stock_price WHERE stock_code = :code " +
            "ORDER BY collected_at DESC LIMIT :limit", nativeQuery = true)
    List<StockPriceEntity> findRecentByCode(@Param("code") String code,
                                            @Param("limit") int limit);
}
