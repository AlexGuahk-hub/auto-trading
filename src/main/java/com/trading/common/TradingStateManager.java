package com.trading.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class TradingStateManager {

    private final AtomicBoolean kisEnabled    = new AtomicBoolean(true);
    private final AtomicBoolean upbitEnabled  = new AtomicBoolean(true);

    public boolean isKisEnabled()   { return kisEnabled.get(); }
    public boolean isUpbitEnabled() { return upbitEnabled.get(); }

    public void setKisEnabled(boolean enabled) {
        kisEnabled.set(enabled);
        log.info("[상태] KIS 자동매매 {}", enabled ? "시작" : "중지");
    }

    public void setUpbitEnabled(boolean enabled) {
        upbitEnabled.set(enabled);
        log.info("[상태] 업비트 자동매매 {}", enabled ? "시작" : "중지");
    }
}
