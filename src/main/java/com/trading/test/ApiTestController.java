package com.trading.test;

import com.trading.kis.auth.KisTokenService;
import com.trading.kis.market.AccountBalanceDto;
import com.trading.kis.market.KisStockService;
import com.trading.kis.market.StockPriceDto;
import com.trading.upbit.market.CoinPriceDto;
import com.trading.upbit.market.UpbitBalanceDto;
import com.trading.upbit.market.UpbitMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * API 연동 테스트용 컨트롤러 — local 프로필에서만 활성화
 *
 * GET /test/kis/token          KIS 토큰 발급 확인
 * GET /test/kis/price/{code}   주식 현재가 (예: 005930)
 * GET /test/kis/balance        주식 계좌 잔고
 * GET /test/upbit/price/{market} 코인 현재가 (예: KRW-BTC)
 * GET /test/upbit/balance      업비트 KRW 잔고
 * GET /test/upbit/accounts     업비트 전체 보유 자산
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
@Profile({"local", "prod"})
public class ApiTestController {

    private final KisTokenService kisTokenService;
    private final KisStockService kisStockService;
    private final UpbitMarketService upbitMarketService;

    // ── KIS 주식 ──

    @GetMapping("/kis/token")
    public Map<String, String> kisToken() {
        String token = kisTokenService.getAccessToken();
        // 토큰 앞 20자만 출력 (보안)
        return Map.of(
            "status", "success",
            "token_preview", token.substring(0, Math.min(20, token.length())) + "..."
        );
    }

    @GetMapping("/kis/price/{code}")
    public StockPriceDto kisPrice(@PathVariable String code) {
        log.info("[테스트] KIS 시세 조회: {}", code);
        return kisStockService.getCurrentPrice(code);
    }

    @GetMapping("/kis/balance")
    public AccountBalanceDto kisBalance() {
        log.info("[테스트] KIS 잔고 조회");
        return kisStockService.getAccountBalance();
    }

    // ── 업비트 코인 ──

    @GetMapping("/upbit/price/{market}")
    public CoinPriceDto upbitPrice(@PathVariable String market) {
        log.info("[테스트] 업비트 시세 조회: {}", market);
        return upbitMarketService.getCurrentPrice(market);
    }

    @GetMapping("/upbit/balance")
    public Map<String, Object> upbitBalance() {
        log.info("[테스트] 업비트 KRW 잔고 조회");
        BigDecimal krw = upbitMarketService.getKrwBalance();
        return Map.of("KRW", krw);
    }

    @GetMapping("/upbit/accounts")
    public List<UpbitBalanceDto> upbitAccounts() {
        log.info("[테스트] 업비트 전체 잔고 조회");
        return upbitMarketService.getBalances();
    }
}
