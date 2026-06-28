package com.trading.control;

import com.trading.common.TradingStateManager;
import com.trading.kis.auth.KisTokenService;
import com.trading.kis.market.AccountBalanceDto;
import com.trading.kis.market.KisStockService;
import com.trading.kis.market.StockPriceDto;
import com.trading.notification.TelegramNotifier;
import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.UpbitBalanceDto;
import com.trading.upbit.market.UpbitMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 자동매매 제어 및 조회 API
 *
 * ── 제어 ──────────────────────────────────────────
 * GET  /trading/status              전체 상태 조회 (활성화 여부 + 잔고)
 * POST /trading/start               전체 자동매매 시작
 * POST /trading/stop                전체 자동매매 중지
 * POST /trading/kis/start           KIS 주식 시작
 * POST /trading/kis/stop            KIS 주식 중지
 * POST /trading/upbit/start         업비트 코인 시작
 * POST /trading/upbit/stop          업비트 코인 중지
 *
 * ── KIS 조회 ──────────────────────────────────────
 * GET  /trading/kis/token           KIS 토큰 발급 확인
 * GET  /trading/kis/price/{code}    주식 현재가 (예: 005930)
 * GET  /trading/kis/balance         주식 계좌 잔고
 *
 * ── 업비트 조회 ───────────────────────────────────
 * GET  /trading/upbit/price/{market} 코인 현재가 (예: KRW-BTC)
 * GET  /trading/upbit/balance        업비트 KRW 잔고
 * GET  /trading/upbit/accounts       업비트 전체 보유 자산
 *
 * ── 관심종목 관리 (→ WatchStockController) ─────────
 * GET    /trading/watchlist/stocks              전체 종목 조회
 * POST   /trading/watchlist/stocks              종목 추가
 * PUT    /trading/watchlist/stocks/{stockCode}  종목 수정
 * DELETE /trading/watchlist/stocks/{stockCode}  종목 삭제
 * PATCH  /trading/watchlist/stocks/{stockCode}/enable   활성화
 * PATCH  /trading/watchlist/stocks/{stockCode}/disable  비활성화
 *
 * ── 코인 관심종목 관리 (→ WatchCoinController) ──────
 * GET    /trading/watchlist/coins              전체 종목 조회
 * POST   /trading/watchlist/coins              종목 추가
 * PUT    /trading/watchlist/coins/{market}     종목 수정
 * DELETE /trading/watchlist/coins/{market}     종목 삭제
 * PATCH  /trading/watchlist/coins/{market}/enable   활성화
 * PATCH  /trading/watchlist/coins/{market}/disable  비활성화
 */
@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
@Slf4j
public class TradingControlController {

    private final TradingStateManager stateManager;
    private final TelegramNotifier    notifier;
    private final KisTokenService     kisTokenService;
    private final KisStockService     kisStockService;
    private final UpbitMarketService  upbitMarketService;

    // ── 상태 조회 ──

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "kis",   Map.of("enabled", stateManager.isKisEnabled()),
            "upbit", Map.of("enabled", stateManager.isUpbitEnabled())
        );
    }

    // ── 전체 제어 ──

    @PostMapping("/start")
    public Map<String, String> startAll() {
        stateManager.setKisEnabled(true);
        stateManager.setUpbitEnabled(true);
        notifier.sendMessage("[제어] 전체 자동매매 시작");
        return Map.of("result", "started");
    }

    @PostMapping("/stop")
    public Map<String, String> stopAll() {
        stateManager.setKisEnabled(false);
        stateManager.setUpbitEnabled(false);
        notifier.sendMessage("[제어] 전체 자동매매 중지");
        return Map.of("result", "stopped");
    }

    // ── KIS 제어 ──

    @PostMapping("/kis/start")
    public Map<String, String> startKis() {
        stateManager.setKisEnabled(true);
        notifier.sendMessage("[제어] KIS 주식 자동매매 시작");
        return Map.of("result", "kis started");
    }

    @PostMapping("/kis/stop")
    public Map<String, String> stopKis() {
        stateManager.setKisEnabled(false);
        notifier.sendMessage("[제어] KIS 주식 자동매매 중지");
        return Map.of("result", "kis stopped");
    }

    // ── KIS 조회 ──

    @GetMapping("/kis/token")
    public Map<String, String> kisToken() {
        String token = kisTokenService.getAccessToken();
        return Map.of(
            "status", "success",
            "token_preview", token.substring(0, Math.min(20, token.length())) + "..."
        );
    }

    @GetMapping("/kis/price/{code}")
    public StockPriceDto kisPrice(@PathVariable String code) {
        log.info("[조회] KIS 시세: {}", code);
        return kisStockService.getCurrentPrice(code);
    }

    @GetMapping("/kis/balance")
    public AccountBalanceDto kisBalance() {
        log.info("[조회] KIS 잔고");
        return kisStockService.getAccountBalance();
    }

    // ── 업비트 제어 ──

    @PostMapping("/upbit/start")
    public Map<String, String> startUpbit() {
        stateManager.setUpbitEnabled(true);
        notifier.sendMessage("[제어] 업비트 코인 자동매매 시작");
        return Map.of("result", "upbit started");
    }

    @PostMapping("/upbit/stop")
    public Map<String, String> stopUpbit() {
        stateManager.setUpbitEnabled(false);
        notifier.sendMessage("[제어] 업비트 코인 자동매매 중지");
        return Map.of("result", "upbit stopped");
    }

    // ── 업비트 조회 ──

    @GetMapping("/upbit/price/{market}")
    public CoinPriceDto upbitPrice(@PathVariable String market) {
        log.info("[조회] 업비트 시세: {}", market);
        return upbitMarketService.getCurrentPrice(market);
    }

    @GetMapping("/upbit/balance")
    public Map<String, Object> upbitBalance() {
        log.info("[조회] 업비트 KRW 잔고");
        BigDecimal krw = upbitMarketService.getKrwBalance();
        return Map.of("KRW", krw);
    }

    @GetMapping("/upbit/accounts")
    public List<UpbitBalanceDto> upbitAccounts() {
        log.info("[조회] 업비트 전체 잔고");
        return upbitMarketService.getBalances();
    }
}
