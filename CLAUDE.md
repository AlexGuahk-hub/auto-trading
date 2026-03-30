# 자동 주식 + 코인 트레이딩 시스템
# KIS API (주식) + 업비트 API (코인) + Spring Boot

## 프로젝트 개요

한국투자증권(KIS) Open API와 업비트 Open API를 동시에 연동한
Spring Boot 기반 자동 트레이딩 시스템.

- **주식**: KIS API — 평일 09:00~15:30 자동매매
- **코인**: 업비트 API — 24시간 365일 자동매매
- 공통 전략 인터페이스로 전략 코드 재사용
- DigitalOcean Docker 배포, GitHub Actions CI/CD

---

## 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Build**: Gradle
- **DB**: PostgreSQL 15
- **Cache**: Redis 7
- **Deploy**: Docker + DigitalOcean ($6/월)
- **CI/CD**: GitHub Actions
- **Notification**: 텔레그램 봇

---

## 디렉토리 구조

```
auto-trading/
├── src/main/java/com/trading/
│   ├── common/                        # 공통 인터페이스/DTO
│   │   ├── TradingStrategy.java       # 전략 인터페이스 (KIS+업비트 공용)
│   │   ├── TradeSignal.java           # BUY / SELL / HOLD enum
│   │   ├── OrderResult.java
│   │   └── RiskCheckResult.java
│   │
│   ├── kis/                           # ── 주식 (KIS) ──
│   │   ├── config/
│   │   │   ├── KisProperties.java
│   │   │   └── KisWebClientConfig.java
│   │   ├── auth/
│   │   │   └── KisTokenService.java   # OAuth2 토큰 (Redis 캐싱 23h)
│   │   ├── market/
│   │   │   └── KisStockService.java   # 시세조회, 잔고조회
│   │   ├── order/
│   │   │   └── KisOrderService.java   # 매수/매도 실행
│   │   ├── collector/
│   │   │   └── KisDataCollector.java  # @Scheduled 시세 수집
│   │   └── scheduler/
│   │       └── KisTradingScheduler.java # 장중 자동매매
│   │
│   ├── upbit/                         # ── 코인 (업비트) ──
│   │   ├── config/
│   │   │   ├── UpbitProperties.java
│   │   │   └── UpbitWebClientConfig.java
│   │   ├── auth/
│   │   │   └── UpbitJwtProvider.java  # 매 요청마다 JWT 생성
│   │   ├── market/
│   │   │   └── UpbitMarketService.java # 시세조회, 잔고조회, 캔들
│   │   ├── order/
│   │   │   └── UpbitOrderService.java  # 매수/매도 실행
│   │   ├── collector/
│   │   │   └── UpbitDataCollector.java # @Scheduled 시세 수집
│   │   └── scheduler/
│   │       └── UpbitTradingScheduler.java # 24시간 자동매매
│   │
│   ├── strategy/                      # 공통 전략 (KIS+업비트 모두 사용)
│   │   ├── MovingAverageStrategy.java # 이동평균 골든크로스
│   │   ├── RsiStrategy.java           # RSI 과매수/과매도
│   │   ├── BollingerBandStrategy.java # 볼린저밴드
│   │   └── VolatilityBreakoutStrategy.java # 변동성 돌파 (코인 특화)
│   │
│   ├── risk/
│   │   └── RiskManager.java           # 손절/포지션/일일손실 관리
│   │
│   └── notification/
│       └── TelegramNotifier.java      # 매매 알림 + 원격 제어
│
├── src/main/resources/
│   ├── application.yml
│   └── application-prod.yml
├── CLAUDE.md                          # 이 파일 (Claude Code 컨텍스트)
├── docker-compose.yml
├── Dockerfile
└── .github/workflows/deploy.yml
```

---

## 중요 규칙 (Claude Code가 반드시 지켜야 할 사항)

1. API 키는 환경변수로만 관리 — 코드에 절대 하드코딩 금지
2. 모든 주문 전 `RiskManager.checkOrder()` 호출 필수
3. 실전 전환 전 KIS 모의투자 2주 이상 테스트 필수
4. 업비트는 모의투자 없음 — 반드시 소액(10,000원)으로 시작
5. 업비트 주문 시 JWT는 매 요청마다 새로 생성 (캐싱 금지)
6. KIS Rate Limit: 초당 5회 → `Thread.sleep(200)` 필수
7. 업비트 Rate Limit: 주문 초당 8회, 주문외 초당 30회

---

## Phase 1: 공통 설정

### 1-1. build.gradle 전체 의존성

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    // 업비트 JWT 인증용
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly  'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly  'io.jsonwebtoken:jjwt-jackson:0.12.6'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    runtimeOnly 'org.postgresql:postgresql'
}
```

### 1-2. application.yml 전체 설정

```yaml
# ── KIS (주식) ──
kis:
  app-key: ${KIS_APP_KEY}
  app-secret: ${KIS_APP_SECRET}
  account-no: ${KIS_ACCOUNT_NO}           # 계좌번호 앞 8자리
  account-product-code: "01"              # 일반 위탁계좌
  base-url: ${KIS_BASE_URL:https://openapivts.koreainvestment.com:29443}
  is-paper: ${KIS_IS_PAPER:true}          # true=모의투자, false=실전

# ── 업비트 (코인) ──
upbit:
  access-key: ${UPBIT_ACCESS_KEY}
  secret-key:  ${UPBIT_SECRET_KEY}
  base-url: https://api.upbit.com

# ── 공통 매매 설정 ──
trading:
  # 주식 관심종목
  stocks:
    - "005930"    # 삼성전자
    - "000660"    # SK하이닉스
    - "035420"    # NAVER
  stock-order-quantity: 1       # 1회 주문 수량 (주)

  # 코인 관심종목
  coins:
    - "KRW-BTC"   # 비트코인
    - "KRW-ETH"   # 이더리움
    - "KRW-XRP"   # 리플
  coin-order-amount-krw: 10000  # 1회 주문금액 (원)

  # 리스크 공통
  stop-loss-ratio: 0.03         # 손절 -3%
  daily-loss-limit: 0.05        # 일일 손실 한도 -5%
  max-position-ratio: 0.10      # 포트폴리오 대비 10% 이내

# ── 텔레그램 ──
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  chat-id:   ${TELEGRAM_CHAT_ID}

# ── Spring ──
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/trading
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  redis:
    host: redis
    port: 6379
  data:
    redis:
      repositories:
        enabled: false
```

### 1-3. 공통 인터페이스 (TradingStrategy.java)

```java
// 주식과 코인이 동일한 인터페이스 사용 → 전략 재사용
public interface TradingStrategy {
    String getName();
    TradeSignal analyze(String market, List<PriceDto> prices);
}

public enum TradeSignal { BUY, SELL, HOLD }
```

---

## Phase 2: KIS 주식 연동

### 2-1. KisProperties.java

```java
@ConfigurationProperties(prefix = "kis")
@Component
@Data
public class KisProperties {
    private String appKey;
    private String appSecret;
    private String accountNo;
    private String accountProductCode;
    private String baseUrl;
    private boolean isPaper;
}
```

### 2-2. KisWebClientConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class KisWebClientConfig {
    private final KisProperties props;

    @Bean("kisWebClient")
    public WebClient kisWebClient() {
        return WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
}
```

### 2-3. KisTokenService.java (OAuth2 토큰 — Redis 23시간 캐싱)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class KisTokenService {

    private final KisProperties props;
    private final WebClient kisWebClient;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_KEY = "kis:access_token";

    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) return cached;

        Map<String, String> body = Map.of(
            "grant_type", "client_credentials",
            "appkey",     props.getAppKey(),
            "appsecret",  props.getAppSecret()
        );

        KisTokenResponse resp = kisWebClient.post()
            .uri("/oauth2/tokenP")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(KisTokenResponse.class)
            .block();

        // 만료 30분 전 갱신을 위해 23시간만 캐싱
        redisTemplate.opsForValue().set(TOKEN_KEY, resp.getAccessToken(), Duration.ofHours(23));
        log.info("KIS 액세스 토큰 발급 완료");
        return resp.getAccessToken();
    }
}
```

### 2-4. KisStockService.java (시세/잔고 조회)

```java
@Service
@RequiredArgsConstructor
public class KisStockService {

    private final KisProperties props;
    private final KisTokenService tokenService;
    private final WebClient kisWebClient;

    // 주식 현재가 조회
    public StockPriceDto getCurrentPrice(String stockCode) {
        return kisWebClient.get()
            .uri(u -> u.path("/uapi/domestic-stock/v1/quotations/inquire-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build())
            .headers(this::setCommonHeaders)
            .header("tr_id", "FHKST01010100")
            .retrieve()
            .bodyToMono(KisStockPriceResponse.class)
            .map(resp -> StockPriceDto.builder()
                .code(stockCode)
                .currentPrice(Long.parseLong(resp.getOutput().getStckPrpr()))
                .openPrice(Long.parseLong(resp.getOutput().getStckOprc()))
                .highPrice(Long.parseLong(resp.getOutput().getStckHgpr()))
                .lowPrice(Long.parseLong(resp.getOutput().getStckLwpr()))
                .volume(Long.parseLong(resp.getOutput().getAcmlVol()))
                .changeRate(new BigDecimal(resp.getOutput().getPrdyCtrt()))
                .build())
            .block();
    }

    // 계좌 잔고 조회
    public AccountBalanceDto getAccountBalance() {
        String trId = props.isPaper() ? "VTTC8434R" : "TTTC8434R";

        return kisWebClient.get()
            .uri(u -> u.path("/uapi/domestic-stock/v1/trading/inquire-balance")
                .queryParam("CANO",               props.getAccountNo())
                .queryParam("ACNT_PRDT_CD",        props.getAccountProductCode())
                .queryParam("AFHR_FLPR_YN",        "N")
                .queryParam("OFL_YN",              "N")
                .queryParam("INQR_DVSN",           "02")
                .queryParam("UNPR_DVSN",           "01")
                .queryParam("FUND_STTL_ICLD_YN",   "N")
                .queryParam("FNCG_AMT_AUTO_RDPT_YN","N")
                .queryParam("PRCS_DVSN",           "00")
                .queryParam("CTX_AREA_FK100",       "")
                .queryParam("CTX_AREA_NK100",       "")
                .build())
            .headers(this::setCommonHeaders)
            .header("tr_id", trId)
            .retrieve()
            .bodyToMono(KisBalanceResponse.class)
            .map(this::mapToBalanceDto)
            .block();
    }

    private void setCommonHeaders(HttpHeaders headers) {
        headers.set("authorization", "Bearer " + tokenService.getAccessToken());
        headers.set("appkey",    props.getAppKey());
        headers.set("appsecret", props.getAppSecret());
    }
}
```

### 2-5. KisOrderService.java (주문 실행)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class KisOrderService {

    private final KisProperties props;
    private final KisTokenService tokenService;
    private final WebClient kisWebClient;
    private final OrderRepository orderRepository;
    private final TelegramNotifier notifier;

    public OrderResult buyMarket(String stockCode, int quantity) {
        return placeOrder(stockCode, quantity, "01", "BUY", "0");
    }

    public OrderResult sellMarket(String stockCode, int quantity) {
        return placeOrder(stockCode, quantity, "01", "SELL", "0");
    }

    public OrderResult buyLimit(String stockCode, int quantity, long price) {
        return placeOrder(stockCode, quantity, "00", "BUY", String.valueOf(price));
    }

    private OrderResult placeOrder(String stockCode, int quantity,
                                   String orderDivision, String side, String price) {
        // 모의/실전 TR ID 분기
        String trId;
        if (props.isPaper()) {
            trId = "BUY".equals(side) ? "VTTC0802U" : "VTTC0801U";
        } else {
            trId = "BUY".equals(side) ? "TTTC0802U" : "TTTC0801U";
        }

        Map<String, String> body = Map.of(
            "CANO",         props.getAccountNo(),
            "ACNT_PRDT_CD", props.getAccountProductCode(),
            "PDNO",         stockCode,
            "ORD_DVSN",     orderDivision,   // 00:지정가, 01:시장가
            "ORD_QTY",      String.valueOf(quantity),
            "ORD_UNPR",     price
        );

        KisOrderResponse resp = kisWebClient.post()
            .uri("/uapi/domestic-stock/v1/trading/order-cash")
            .headers(h -> {
                h.set("authorization", "Bearer " + tokenService.getAccessToken());
                h.set("appkey",    props.getAppKey());
                h.set("appsecret", props.getAppSecret());
                h.set("tr_id",     trId);
                h.set("custtype",  "P");
            })
            .bodyValue(body)
            .retrieve()
            .bodyToMono(KisOrderResponse.class)
            .block();

        OrderResult result = OrderResult.builder()
            .exchange("KIS")
            .market(stockCode)
            .side(side)
            .quantity(quantity)
            .orderId(resp.getOutput().getKstnOrdno())
            .success("0".equals(resp.getRtCd()))
            .build();

        orderRepository.save(OrderEntity.from(result));
        notifier.sendOrderNotification(result);
        log.info("[KIS] 주문: {} {} {}주 - orderId: {}", side, stockCode, quantity, result.getOrderId());
        return result;
    }
}
```

### 2-6. KisDataCollector.java (시세 수집 스케줄러)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class KisDataCollector {

    private final KisStockService stockService;
    private final StockPriceRepository priceRepository;

    @Value("${trading.stocks}")
    private List<String> watchList;

    // 장중 1분마다 시세 수집 (평일 09:00~15:30)
    @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void collectPrices() {
        watchList.forEach(code -> {
            try {
                StockPriceDto price = stockService.getCurrentPrice(code);
                priceRepository.save(StockPriceEntity.from(price));
                Thread.sleep(200); // KIS Rate Limit: 초당 5회 제한
            } catch (Exception e) {
                log.error("[KIS] 시세 수집 실패: {}", code, e);
            }
        });
    }
}
```

### 2-7. KisTradingScheduler.java (자동매매)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class KisTradingScheduler {

    private final KisStockService      stockService;
    private final KisOrderService      orderService;
    private final RiskManager          riskManager;
    private final List<TradingStrategy> strategies;
    private final StockPriceRepository priceRepository;

    @Value("${trading.stocks}")
    private List<String> watchList;

    @Value("${trading.stock-order-quantity}")
    private int orderQuantity;

    // 장중 5분마다 전략 실행 (09:05~15:20, 마감 10분 전 종료)
    @Scheduled(cron = "0 5-20/5 9-14,15 * * MON-FRI", zone = "Asia/Seoul")
    public void runStrategy() {
        log.info("[KIS] 자동매매 전략 실행");
        watchList.forEach(code -> {
            try {
                analyzeAndTrade(code);
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("[KIS] 전략 실행 오류: {}", code, e);
            }
        });
    }

    private void analyzeAndTrade(String stockCode) {
        List<PriceDto> prices = priceRepository.findRecentPrices(stockCode, 60);
        if (prices.size() < 20) return;

        for (TradingStrategy strategy : strategies) {
            TradeSignal signal = strategy.analyze(stockCode, prices);
            if (signal == TradeSignal.HOLD) continue;

            long currentPrice = prices.get(prices.size() - 1).getCurrentPrice();
            RiskCheckResult risk = riskManager.checkOrder(stockCode, orderQuantity, currentPrice);
            if (!risk.isApproved()) {
                log.warn("[KIS] 리스크 거부 [{}]: {}", stockCode, risk.getReason());
                continue;
            }

            if (signal == TradeSignal.BUY)  orderService.buyMarket(stockCode, orderQuantity);
            else                             orderService.sellMarket(stockCode, orderQuantity);
        }
    }
}
```

---

## Phase 3: 업비트 코인 연동

### 3-1. UpbitProperties.java

```java
@ConfigurationProperties(prefix = "upbit")
@Component
@Data
public class UpbitProperties {
    private String accessKey;
    private String secretKey;
    private String baseUrl;
}
```

### 3-2. UpbitWebClientConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class UpbitWebClientConfig {
    private final UpbitProperties props;

    @Bean("upbitWebClient")
    public WebClient upbitWebClient() {
        return WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            // Remaining-Req 헤더 모니터링 필터
            .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                String remaining = resp.headers().asHttpHeaders().getFirst("Remaining-Req");
                if (remaining != null) log.debug("[업비트] Rate Limit: {}", remaining);
                return Mono.just(resp);
            }))
            .build();
    }
}
```

### 3-3. UpbitJwtProvider.java (핵심 — 매 요청마다 JWT 생성)

```java
// 업비트는 KIS와 달리 매 요청마다 JWT를 새로 생성해야 함 (캐싱 불가)
@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitJwtProvider {

    private final UpbitProperties props;

    // 파라미터 없는 요청 (잔고 조회 등)
    public String createToken() {
        return buildJwt(null);
    }

    // 파라미터 있는 요청 (주문 등) — 쿼리 해시 필수
    public String createToken(MultiValueMap<String, String> params) {
        String queryString = params.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + "=" + v))
            .collect(Collectors.joining("&"));
        return buildJwt(sha512(queryString));
    }

    private String buildJwt(String queryHash) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("access_key", props.getAccessKey());
        claims.put("nonce",      UUID.randomUUID().toString());
        if (queryHash != null) {
            claims.put("query_hash",     queryHash);
            claims.put("query_hash_alg", "SHA512");
        }
        return Jwts.builder()
            .claims(claims)
            .signWith(Keys.hmacShaKeyFor(props.getSecretKey().getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 unavailable", e);
        }
    }
}
```

### 3-4. UpbitMarketService.java (시세/잔고/캔들 조회)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UpbitMarketService {

    private final WebClient upbitWebClient;
    private final UpbitJwtProvider jwtProvider;

    // 현재가 조회 (인증 불필요 — Public API)
    public CoinPriceDto getCurrentPrice(String market) {
        return upbitWebClient.get()
            .uri(u -> u.path("/v1/ticker")
                .queryParam("markets", market)
                .build())
            .retrieve()
            .bodyToFlux(UpbitTickerResponse.class)
            .map(r -> CoinPriceDto.builder()
                .market(market)
                .currentPrice(r.getTradePrice())
                .openPrice(r.getOpeningPrice())
                .highPrice(r.getHighPrice())
                .lowPrice(r.getLowPrice())
                .volume(r.getAccTradeVolume())
                .changeRate(r.getSignedChangeRate())
                .build())
            .blockFirst();
    }

    // 분봉 캔들 조회 (최대 200개)
    public List<CoinPriceDto> getMinuteCandles(String market, int unit, int count) {
        return upbitWebClient.get()
            .uri(u -> u.path("/v1/candles/minutes/" + unit)
                .queryParam("market", market)
                .queryParam("count",  Math.min(count, 200))
                .build())
            .retrieve()
            .bodyToFlux(UpbitCandleResponse.class)
            .map(r -> CoinPriceDto.builder()
                .market(market)
                .currentPrice(r.getTradePrice())
                .openPrice(r.getOpeningPrice())
                .highPrice(r.getHighPrice())
                .lowPrice(r.getLowPrice())
                .volume(r.getCandleAccTradeVolume())
                .build())
            .collectList()
            .map(list -> { Collections.reverse(list); return list; }) // 오름차순 정렬
            .block();
    }

    // KRW 보유 잔고 조회 (인증 필요)
    public BigDecimal getKrwBalance() {
        return getBalances().stream()
            .filter(b -> "KRW".equals(b.getCurrency()))
            .map(b -> new BigDecimal(b.getBalance()))
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }

    // 특정 코인 보유 수량 조회
    public BigDecimal getHoldingVolume(String market) {
        String currency = market.replace("KRW-", "");
        return getBalances().stream()
            .filter(b -> currency.equals(b.getCurrency()))
            .map(b -> new BigDecimal(b.getBalance()))
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }

    // 전체 잔고 조회 (인증 필요)
    public List<UpbitBalanceDto> getBalances() {
        return upbitWebClient.get()
            .uri("/v1/accounts")
            .header("Authorization", "Bearer " + jwtProvider.createToken())
            .retrieve()
            .bodyToFlux(UpbitBalanceDto.class)
            .collectList()
            .block();
    }
}
```

### 3-5. UpbitOrderService.java (주문 실행)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UpbitOrderService {

    private final WebClient         upbitWebClient;
    private final UpbitJwtProvider  jwtProvider;
    private final OrderRepository   orderRepository;
    private final TelegramNotifier  notifier;

    // 시장가 매수 (원화 금액 기준) — ord_type: price
    public OrderResult buyMarket(String market, BigDecimal krwAmount) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market",   market);
        params.add("side",     "bid");
        params.add("price",    krwAmount.toPlainString());
        params.add("ord_type", "price");
        // 시장가 매수는 volume 파라미터 제외
        return placeOrder(market, "BUY", params);
    }

    // 시장가 매도 (코인 수량 기준) — ord_type: market
    public OrderResult sellMarket(String market, BigDecimal volume) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market",   market);
        params.add("side",     "ask");
        params.add("volume",   volume.toPlainString());
        params.add("ord_type", "market");
        // 시장가 매도는 price 파라미터 제외
        return placeOrder(market, "SELL", params);
    }

    // 지정가 매수
    public OrderResult buyLimit(String market, BigDecimal volume, BigDecimal price) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("market",   market);
        params.add("side",     "bid");
        params.add("volume",   volume.toPlainString());
        params.add("price",    price.toPlainString());
        params.add("ord_type", "limit");
        return placeOrder(market, "BUY", params);
    }

    private OrderResult placeOrder(String market, String side,
                                   MultiValueMap<String, String> params) {
        // 파라미터 기반 JWT 생성 (쿼리 해시 포함) — 매번 새로 생성
        String jwt = jwtProvider.createToken(params);

        UpbitOrderResponse resp = upbitWebClient.post()
            .uri("/v1/orders")
            .header("Authorization", "Bearer " + jwt)
            .bodyValue(params.toSingleValueMap())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, r ->
                r.bodyToMono(String.class)
                 .map(body -> new RuntimeException("[업비트] 주문 오류: " + body)))
            .bodyToMono(UpbitOrderResponse.class)
            .block();

        OrderResult result = OrderResult.builder()
            .exchange("UPBIT")
            .market(market)
            .side(side)
            .orderId(resp.getUuid())
            .success(resp.getUuid() != null)
            .build();

        orderRepository.save(OrderEntity.from(result));
        notifier.sendOrderNotification(result);
        log.info("[업비트] 주문: {} {} - uuid: {}", side, market, resp.getUuid());
        return result;
    }
}
```

### 3-6. UpbitDataCollector.java (시세 수집)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitDataCollector {

    private final UpbitMarketService marketService;
    private final CoinPriceRepository priceRepository;

    @Value("${trading.coins}")
    private List<String> coins;

    // 5분마다 시세 수집 (24시간 365일)
    @Scheduled(fixedDelay = 300_000)
    public void collectPrices() {
        coins.forEach(market -> {
            try {
                CoinPriceDto price = marketService.getCurrentPrice(market);
                priceRepository.save(CoinPriceEntity.from(price));
                Thread.sleep(150); // 업비트 Rate Limit 준수
            } catch (Exception e) {
                log.error("[업비트] 시세 수집 실패: {}", market, e);
            }
        });
    }
}
```

### 3-7. UpbitTradingScheduler.java (24시간 자동매매)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitTradingScheduler {

    private final UpbitMarketService  marketService;
    private final UpbitOrderService   orderService;
    private final RiskManager         riskManager;
    private final CoinPriceRepository priceRepository;

    @Value("${trading.coins}")
    private List<String> coins;

    @Value("${trading.coin-order-amount-krw}")
    private BigDecimal orderAmountKrw;

    // 5분마다 전략 실행 (24시간 365일 — 코인은 시간 제한 없음)
    @Scheduled(fixedDelay = 300_000)
    public void runStrategy() {
        log.info("[업비트] 자동매매 전략 실행");
        coins.forEach(market -> {
            try {
                analyzeAndTrade(market);
                Thread.sleep(150);
            } catch (Exception e) {
                log.error("[업비트] 전략 실행 오류: {}", market, e);
            }
        });
    }

    private void analyzeAndTrade(String market) {
        // 1시간봉 48개 조회 (2일치)
        List<CoinPriceDto> candles = marketService.getMinuteCandles(market, 60, 48);
        if (candles.size() < 20) return;

        // 변동성 돌파 전략 (코인에 특화)
        TradeSignal signal = volatilityBreakout(market, candles);

        if (signal == TradeSignal.HOLD) return;

        // 리스크 검사
        long currentPrice = candles.get(candles.size() - 1).getCurrentPrice();
        RiskCheckResult risk = riskManager.checkCoinOrder(market, orderAmountKrw, currentPrice);
        if (!risk.isApproved()) {
            log.warn("[업비트] 리스크 거부 [{}]: {}", market, risk.getReason());
            return;
        }

        if (signal == TradeSignal.BUY) {
            orderService.buyMarket(market, orderAmountKrw);
        } else {
            BigDecimal holding = marketService.getHoldingVolume(market);
            if (holding.compareTo(BigDecimal.ZERO) > 0) {
                orderService.sellMarket(market, holding);
            }
        }
    }

    // 래리 윌리엄스 변동성 돌파 전략 (k=0.5)
    private TradeSignal volatilityBreakout(String market, List<CoinPriceDto> candles) {
        CoinPriceDto prev    = candles.get(candles.size() - 2);
        CoinPriceDto current = candles.get(candles.size() - 1);

        double range       = prev.getHighPrice() - prev.getLowPrice();
        double target      = prev.getOpenPrice() + range * 0.5;
        double curPrice    = current.getCurrentPrice();

        if (curPrice >= target) {
            log.info("[업비트] 변동성 돌파 [{}] target:{} current:{}", market, target, curPrice);
            return TradeSignal.BUY;
        }
        return TradeSignal.HOLD;
    }
}
```

---

## Phase 4: 공통 전략

### 4-1. MovingAverageStrategy.java (이동평균 골든크로스 — KIS + 업비트 공용)

```java
@Component
@Slf4j
public class MovingAverageStrategy implements TradingStrategy {

    private static final int SHORT = 5;
    private static final int LONG  = 20;

    @Override public String getName() { return "MovingAverage"; }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        if (prices.size() < LONG) return TradeSignal.HOLD;

        double shortMa     = avg(prices, SHORT);
        double longMa      = avg(prices, LONG);
        double prevShortMa = avg(prices.subList(0, prices.size() - 1), SHORT);
        double prevLongMa  = avg(prices.subList(0, prices.size() - 1), LONG);

        if (prevShortMa <= prevLongMa && shortMa > longMa) {
            log.info("[{}] 골든크로스 5MA:{} > 20MA:{}", market, shortMa, longMa);
            return TradeSignal.BUY;
        }
        if (prevShortMa >= prevLongMa && shortMa < longMa) {
            log.info("[{}] 데드크로스 5MA:{} < 20MA:{}", market, shortMa, longMa);
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }

    private double avg(List<PriceDto> prices, int period) {
        return prices.stream()
            .skip(Math.max(0, prices.size() - period))
            .mapToDouble(PriceDto::getCurrentPrice)
            .average().orElse(0);
    }
}
```

### 4-2. RsiStrategy.java (KIS + 업비트 공용)

```java
@Component
@Slf4j
public class RsiStrategy implements TradingStrategy {

    private static final int    PERIOD     = 14;
    private static final double OVERSOLD   = 30.0;
    private static final double OVERBOUGHT = 70.0;

    @Override public String getName() { return "RSI"; }

    @Override
    public TradeSignal analyze(String market, List<PriceDto> prices) {
        double rsi = calcRsi(prices, PERIOD);
        log.debug("[{}] RSI: {}", market, rsi);
        if (rsi < OVERSOLD)   return TradeSignal.BUY;
        if (rsi > OVERBOUGHT) return TradeSignal.SELL;
        return TradeSignal.HOLD;
    }

    private double calcRsi(List<PriceDto> prices, int period) {
        if (prices.size() < period + 1) return 50.0;
        double gain = 0, loss = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double diff = prices.get(i).getCurrentPrice() - prices.get(i - 1).getCurrentPrice();
            if (diff > 0) gain += diff; else loss -= diff;
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0) return 100.0;
        return 100 - (100 / (1 + avgGain / avgLoss));
    }
}
```

---

## Phase 5: 리스크 관리

### 5-1. RiskManager.java

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class RiskManager {

    @Value("${trading.stop-loss-ratio}")      private double stopLossRatio;
    @Value("${trading.daily-loss-limit}")     private double dailyLossLimit;
    @Value("${trading.max-position-ratio}")   private double maxPositionRatio;

    private final KisStockService    kisService;
    private final UpbitMarketService upbitService;
    private final OrderRepository    orderRepository;

    // 주식 주문 가능 여부 검사
    public RiskCheckResult checkOrder(String stockCode, int quantity, long price) {
        long orderAmount = price * quantity;
        AccountBalanceDto balance = kisService.getAccountBalance();

        double positionRatio = (double) orderAmount / balance.getTotalAsset();
        if (positionRatio > maxPositionRatio)
            return RiskCheckResult.reject(String.format("포지션 한도 초과 %.1f%%", positionRatio * 100));

        if (isDailyLossExceeded("KIS"))
            return RiskCheckResult.reject("KIS 일일 손실 한도 초과");

        return RiskCheckResult.approve();
    }

    // 코인 주문 가능 여부 검사
    public RiskCheckResult checkCoinOrder(String market, BigDecimal krwAmount, long currentPrice) {
        BigDecimal krwBalance = upbitService.getKrwBalance();

        if (krwAmount.compareTo(krwBalance) > 0)
            return RiskCheckResult.reject("KRW 잔고 부족");

        if (isDailyLossExceeded("UPBIT"))
            return RiskCheckResult.reject("업비트 일일 손실 한도 초과");

        return RiskCheckResult.approve();
    }

    // 주식 손절 감지 — 5분마다 체크 (장중만)
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void checkKisStopLoss() {
        kisService.getAccountBalance().getHoldings().forEach(h -> {
            double lossRatio = (double)(h.getCurrentPrice() - h.getAvgPrice()) / h.getAvgPrice();
            if (lossRatio <= -stopLossRatio) {
                log.warn("[KIS 손절] {} 손실률: {:.1f}%", h.getStockCode(), lossRatio * 100);
                // KisOrderService.sellMarket() 호출
            }
        });
    }

    // 코인 손절 감지 — 5분마다 체크 (24시간)
    @Scheduled(fixedDelay = 300_000)
    public void checkUpbitStopLoss() {
        upbitService.getBalances().stream()
            .filter(b -> !"KRW".equals(b.getCurrency()) && Double.parseDouble(b.getBalance()) > 0)
            .forEach(b -> {
                String market = "KRW-" + b.getCurrency();
                CoinPriceDto current = upbitService.getCurrentPrice(market);
                double avgPrice  = Double.parseDouble(b.getAvgBuyPrice());
                double curPrice  = current.getCurrentPrice();
                double lossRatio = (curPrice - avgPrice) / avgPrice;

                if (lossRatio <= -stopLossRatio) {
                    log.warn("[업비트 손절] {} 손실률: {:.1f}%", market, lossRatio * 100);
                    // UpbitOrderService.sellMarket() 호출
                }
            });
    }

    private boolean isDailyLossExceeded(String exchange) {
        // 오늘 실현 손실 합산 로직
        return false; // TODO: 구현
    }
}
```

---

## Phase 6: 텔레그램 알림 + 원격 제어

### 6-1. TelegramNotifier.java

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramNotifier {

    @Value("${telegram.bot-token}") private String botToken;
    @Value("${telegram.chat-id}")   private String chatId;

    private final WebClient telegramClient = WebClient.create("https://api.telegram.org");

    // 주식 주문 알림
    public void sendOrderNotification(OrderResult order) {
        String icon   = "BUY".equals(order.getSide()) ? "매수" : "매도";
        String prefix = "KIS".equals(order.getExchange()) ? "[주식]" : "[코인]";
        sendMessage(String.format("%s %s %s 체결\n종목: %s\n주문ID: %s",
            prefix, icon,
            "KIS".equals(order.getExchange()) ? order.getQuantity() + "주" : order.getAmount() + "원",
            order.getMarket(), order.getOrderId()));
    }

    // 일일 리포트 — 평일 16:00 주식 + 매일 자정 코인
    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void sendStockDailyReport() {
        sendMessage("📊 [주식] 오늘 매매 리포트\n수익: 계산 중...");
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void sendCoinDailyReport() {
        sendMessage("📊 [코인] 오늘 매매 리포트\n수익: 계산 중...");
    }

    // 오류 알림
    public void sendErrorAlert(String message) {
        sendMessage("경고: " + message);
    }

    public void sendMessage(String text) {
        telegramClient.post()
            .uri("/bot" + botToken + "/sendMessage")
            .bodyValue(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(null, e -> log.error("텔레그램 전송 실패", e));
    }
}
```

---

## Phase 7: 배포

### 7-1. Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

### 7-2. docker-compose.yml

```yaml
version: '3.8'

services:
  trading-app:
    image: ghcr.io/내아이디/auto-trading:latest
    restart: always
    ports:
      - "8080:8080"
    environment:
      # KIS 주식
      - KIS_APP_KEY=${KIS_APP_KEY}
      - KIS_APP_SECRET=${KIS_APP_SECRET}
      - KIS_ACCOUNT_NO=${KIS_ACCOUNT_NO}
      - KIS_BASE_URL=${KIS_BASE_URL}
      - KIS_IS_PAPER=${KIS_IS_PAPER}
      # 업비트 코인
      - UPBIT_ACCESS_KEY=${UPBIT_ACCESS_KEY}
      - UPBIT_SECRET_KEY=${UPBIT_SECRET_KEY}
      # 공통
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
      - TELEGRAM_CHAT_ID=${TELEGRAM_CHAT_ID}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started

  postgres:
    image: postgres:15-alpine
    restart: always
    environment:
      POSTGRES_DB: trading
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### 7-3. .env (서버에만 보관 — Git 절대 커밋 금지)

```
# KIS 주식
KIS_APP_KEY=your_kis_app_key
KIS_APP_SECRET=your_kis_app_secret
KIS_ACCOUNT_NO=your_account_no_8digits
KIS_BASE_URL=https://openapivts.koreainvestment.com:29443
KIS_IS_PAPER=true

# 업비트 코인
UPBIT_ACCESS_KEY=your_upbit_access_key
UPBIT_SECRET_KEY=your_upbit_secret_key

# DB
DB_USERNAME=trading_user
DB_PASSWORD=strong_db_password
REDIS_PASSWORD=strong_redis_password

# 텔레그램
TELEGRAM_BOT_TOKEN=your_bot_token
TELEGRAM_CHAT_ID=your_chat_id
```

### 7-4. .github/workflows/deploy.yml (DigitalOcean 자동 배포)

```yaml
name: Deploy to DigitalOcean

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build -x test

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push Docker image
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/${{ github.repository }}:latest

      - name: Deploy to DigitalOcean
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.DO_HOST }}
          username: root
          key: ${{ secrets.DO_SSH_KEY }}
          script: |
            cd /root/auto-trading
            docker-compose pull trading-app
            docker-compose up -d --no-deps trading-app
            docker image prune -f
            echo "배포 완료: $(date)"

      - name: Notify Telegram
        run: |
          curl -s -X POST "https://api.telegram.org/bot${{ secrets.TELEGRAM_BOT_TOKEN }}/sendMessage" \
            -d "chat_id=${{ secrets.TELEGRAM_CHAT_ID }}" \
            -d "text=배포 완료 (commit: ${{ github.sha }})"
```

---

## DB 테이블 설계

```sql
-- 주식 시세
CREATE TABLE stock_price (
    id            BIGSERIAL PRIMARY KEY,
    stock_code    VARCHAR(10)    NOT NULL,
    current_price BIGINT         NOT NULL,
    open_price    BIGINT,
    high_price    BIGINT,
    low_price     BIGINT,
    volume        BIGINT,
    change_rate   DECIMAL(6,2),
    collected_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stock_price ON stock_price(stock_code, collected_at DESC);

-- 코인 시세
CREATE TABLE coin_price (
    id            BIGSERIAL PRIMARY KEY,
    market        VARCHAR(20)    NOT NULL,  -- KRW-BTC 형식
    current_price DECIMAL(20,2)  NOT NULL,
    open_price    DECIMAL(20,2),
    high_price    DECIMAL(20,2),
    low_price     DECIMAL(20,2),
    volume        DECIMAL(30,8),
    collected_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_coin_price ON coin_price(market, collected_at DESC);

-- 통합 주문 내역 (주식 + 코인 공용)
CREATE TABLE trade_order (
    id         BIGSERIAL PRIMARY KEY,
    exchange   VARCHAR(10)    NOT NULL,  -- KIS / UPBIT
    market     VARCHAR(20)    NOT NULL,  -- 005930 / KRW-BTC
    side       VARCHAR(4)     NOT NULL,  -- BUY / SELL
    quantity   DECIMAL(20,8),
    amount_krw DECIMAL(20,2),
    price      DECIMAL(20,2),
    order_id   VARCHAR(100),
    strategy   VARCHAR(50),
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_exchange ON trade_order(exchange, created_at DESC);
```

---

## API Rate Limit 정리

| 거래소 | 구분 | 제한 | 대응 |
|--------|------|------|------|
| KIS (주식) | REST API | 초당 5회 | Thread.sleep(200) |
| KIS (주식) | WebSocket | 동시 1개 | — |
| 업비트 (코인) | 주문 API | 초당 8회 / 분당 200회 | Thread.sleep(150) |
| 업비트 (코인) | 주문 외 API | 초당 30회 / 분당 900회 | Remaining-Req 헤더 확인 |
| 업비트 (코인) | Quotation | IP 기준 초당 10회 | — |

---

## KIS vs 업비트 인증 방식 비교

| 항목 | KIS (주식) | 업비트 (코인) |
|------|-----------|------------|
| 방식 | OAuth2 Bearer Token | JWT (매 요청마다 생성) |
| 유효기간 | 24시간 | 단회성 |
| 캐싱 | Redis 23시간 캐싱 가능 | 캐싱 불가 |
| 파라미터 포함 여부 | 불필요 | SHA512 해시 필수 |

---

## 개발 우선순위

### 주식 (KIS)
1. `KisTokenService` → 토큰 발급 테스트
2. `KisStockService.getCurrentPrice("005930")` → 삼성전자 시세 조회
3. `KisStockService.getAccountBalance()` → 잔고 확인
4. `KisOrderService.buyMarket()` → 모의투자 1주 주문
5. `KisDataCollector` → 5분봉 수집 확인
6. `MovingAverageStrategy` → 백테스트 검증
7. 모의투자 2~4주 운용 후 실전 전환

### 코인 (업비트)
1. `UpbitJwtProvider` → JWT 생성 단위 테스트
2. `UpbitMarketService.getCurrentPrice("KRW-BTC")` → BTC 시세 조회
3. `UpbitMarketService.getKrwBalance()` → KRW 잔고 확인
4. `UpbitOrderService.buyMarket("KRW-BTC", 10000)` → 10,000원 소액 테스트
5. `UpbitDataCollector` → 5분봉 수집 확인
6. `VolatilityBreakoutStrategy` → 1~2주 소액 실거래 검증
7. 수익률 확인 후 금액 증액

---

## 보안 체크리스트

- [ ] `.env` 파일 `.gitignore`에 추가 확인
- [ ] GitHub 저장소 Private으로 설정
- [ ] `application.yml` 키 하드코딩 없음 확인
- [ ] DigitalOcean Firewall: 8080 포트 본인 IP만 허용
- [ ] Redis `requirepass` 설정
- [ ] KIS 모의투자 2주 이상 검증 후 실전 전환
- [ ] 업비트 소액(1만원) 테스트 후 금액 증액
- [ ] 일일 손실 한도 설정 (dailyLossLimit)
- [ ] 텔레그램 알림 정상 동작 확인

> ⚠️ 자동매매 시스템은 금전적 손실을 초래할 수 있습니다.
> 주식은 반드시 모의투자로, 코인은 소액으로 충분히 검증한 후 실전 운용하세요.
