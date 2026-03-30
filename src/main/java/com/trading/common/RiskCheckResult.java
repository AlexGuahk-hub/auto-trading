package com.trading.common;

import lombok.Getter;

@Getter
public class RiskCheckResult {
    private final boolean approved;
    private final String reason;

    private RiskCheckResult(boolean approved, String reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static RiskCheckResult approve() {
        return new RiskCheckResult(true, null);
    }

    public static RiskCheckResult reject(String reason) {
        return new RiskCheckResult(false, reason);
    }
}
