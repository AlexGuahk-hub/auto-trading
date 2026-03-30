package com.trading.kis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
