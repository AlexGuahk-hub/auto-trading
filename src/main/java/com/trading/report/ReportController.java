package com.trading.report;

import com.trading.common.OrderEntity;
import com.trading.common.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매매 리포트 조회 API
 *
 * GET /trading/report/orders?from=2024-01-01&to=2024-12-31&exchange=ALL
 *   exchange: ALL(기본) | KIS | UPBIT
 */
@RestController
@RequestMapping("/trading/report")
@RequiredArgsConstructor
public class ReportController {

    private final OrderRepository orderRepository;

    @GetMapping("/orders")
    public List<ReportOrderDto> getOrders(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "ALL") String exchange) {

        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).atTime(23, 59, 59);

        List<OrderEntity> orders = "ALL".equals(exchange)
                ? orderRepository.findByCreatedAtBetween(fromDt, toDt)
                : orderRepository.findByExchangeAndCreatedAtBetween(exchange, fromDt, toDt);

        return orders.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ReportOrderDto::from)
                .toList();
    }
}
