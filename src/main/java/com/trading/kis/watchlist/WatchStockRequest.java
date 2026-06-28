package com.trading.kis.watchlist;

import lombok.Data;

@Data
public class WatchStockRequest {
    /** 6자리 종목코드 (예: 005930) */
    private String stockCode;
    /** 종목명 (예: 삼성전자) */
    private String stockName;
    /** 활성화 여부 — null이면 기본값 true */
    private Boolean enabled;
    /** 1회 주문 수량 — null이면 global default 사용 */
    private Integer orderQty;
    /** 메모 */
    private String note;
}
