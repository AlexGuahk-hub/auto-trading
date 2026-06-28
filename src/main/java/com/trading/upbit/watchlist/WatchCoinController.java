package com.trading.upbit.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 코인 관심종목 관리 API
 *
 * GET    /trading/watchlist/coins              전체 종목 조회
 * POST   /trading/watchlist/coins              종목 추가
 * PUT    /trading/watchlist/coins/{market}     종목 수정
 * DELETE /trading/watchlist/coins/{market}     종목 삭제
 * PATCH  /trading/watchlist/coins/{market}/enable   활성화
 * PATCH  /trading/watchlist/coins/{market}/disable  비활성화
 */
@RestController
@RequestMapping("/trading/watchlist/coins")
@RequiredArgsConstructor
public class WatchCoinController {

    private final WatchCoinService service;

    @GetMapping
    public List<WatchCoinEntity> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchCoinEntity add(@RequestBody WatchCoinRequest req) {
        return service.add(req);
    }

    @PutMapping("/{market}")
    public WatchCoinEntity update(
            @PathVariable String market,
            @RequestBody WatchCoinRequest req) {
        return service.update(market, req);
    }

    @DeleteMapping("/{market}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String market) {
        service.delete(market);
    }

    @PatchMapping("/{market}/enable")
    public WatchCoinEntity enable(@PathVariable String market) {
        return service.setEnabled(market, true);
    }

    @PatchMapping("/{market}/disable")
    public WatchCoinEntity disable(@PathVariable String market) {
        return service.setEnabled(market, false);
    }
}
