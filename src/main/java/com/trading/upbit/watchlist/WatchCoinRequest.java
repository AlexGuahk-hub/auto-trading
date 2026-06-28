package com.trading.upbit.watchlist;

import lombok.Data;

@Data
public class WatchCoinRequest {
    /** KRW-BTC 형식 마켓 코드 */
    private String market;
    /** 코인명 (예: 비트코인) */
    private String coinName;
    /** 활성화 여부 — null이면 기본값 true */
    private Boolean enabled;
    /** 1회 주문금액(원) — null이면 global default(trading.coin-order-amount-krw) 사용 */
    private java.math.BigDecimal orderAmountKrw;
    /** 메모 */
    private String note;
}
