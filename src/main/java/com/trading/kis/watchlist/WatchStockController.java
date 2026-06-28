package com.trading.kis.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주식 관심종목 관리 API
 *
 * GET    /trading/watchlist/stocks              전체 종목 조회
 * POST   /trading/watchlist/stocks              종목 추가
 * PUT    /trading/watchlist/stocks/{stockCode}  종목 수정
 * DELETE /trading/watchlist/stocks/{stockCode}  종목 삭제
 * PATCH  /trading/watchlist/stocks/{stockCode}/enable   활성화
 * PATCH  /trading/watchlist/stocks/{stockCode}/disable  비활성화
 */
@RestController
@RequestMapping("/trading/watchlist/stocks")
@RequiredArgsConstructor
public class WatchStockController {

    private final WatchStockService service;

    @GetMapping
    public List<WatchStockEntity> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchStockEntity add(@RequestBody WatchStockRequest req) {
        return service.add(req);
    }

    @PutMapping("/{stockCode}")
    public WatchStockEntity update(
            @PathVariable String stockCode,
            @RequestBody WatchStockRequest req) {
        return service.update(stockCode, req);
    }

    @DeleteMapping("/{stockCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String stockCode) {
        service.delete(stockCode);
    }

    @PatchMapping("/{stockCode}/enable")
    public WatchStockEntity enable(@PathVariable String stockCode) {
        return service.setEnabled(stockCode, true);
    }

    @PatchMapping("/{stockCode}/disable")
    public WatchStockEntity disable(@PathVariable String stockCode) {
        return service.setEnabled(stockCode, false);
    }
}
