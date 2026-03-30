-- 주식 시세
CREATE TABLE IF NOT EXISTS stock_price (
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
CREATE INDEX IF NOT EXISTS idx_stock_price ON stock_price(stock_code, collected_at DESC);

-- 코인 시세
CREATE TABLE IF NOT EXISTS coin_price (
    id            BIGSERIAL PRIMARY KEY,
    market        VARCHAR(20)    NOT NULL,
    current_price DECIMAL(20,2)  NOT NULL,
    open_price    DECIMAL(20,2),
    high_price    DECIMAL(20,2),
    low_price     DECIMAL(20,2),
    volume        DECIMAL(30,8),
    collected_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_coin_price ON coin_price(market, collected_at DESC);

-- 통합 주문 내역 (주식 + 코인 공용)
CREATE TABLE IF NOT EXISTS trade_order (
    id         BIGSERIAL PRIMARY KEY,
    exchange   VARCHAR(10)    NOT NULL,
    market     VARCHAR(20)    NOT NULL,
    side       VARCHAR(4)     NOT NULL,
    quantity   DECIMAL(20,8),
    amount_krw DECIMAL(20,2),
    price      DECIMAL(20,2),
    order_id   VARCHAR(100),
    strategy   VARCHAR(50),
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_order_exchange ON trade_order(exchange, created_at DESC);
