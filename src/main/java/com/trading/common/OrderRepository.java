package com.trading.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByExchangeAndCreatedAtBetween(
            String exchange, LocalDateTime from, LocalDateTime to);
}
