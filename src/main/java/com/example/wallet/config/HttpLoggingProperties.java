package com.example.wallet.config;

import java.util.List;
import java.util.Set;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wallet.http-log")
public record HttpLoggingProperties(
    Level level,
    int maxBodyLength,
    String replacement,
    Set<String> maskedHeaders,
    Set<String> maskedBodyFields,
    List<String> excludedPaths) {}
