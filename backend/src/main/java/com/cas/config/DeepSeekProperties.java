package com.cas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
public class DeepSeekProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer timeout = 30000;
}
