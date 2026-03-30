package com.trading.upbit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "upbit")
@Component
@Data
public class UpbitProperties {
    private String accessKey;
    private String secretKey;
    private String baseUrl;
}
