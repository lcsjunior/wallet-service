package com.example.wallet.service;

import static java.lang.Boolean.TRUE;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

  private static final String LOG_PREFIX = "[IDEMPOTENCY_SERVICE] ";

  private static final String KEY_FORMAT = "%s:idempotency:%s:%s";

  private static final String RESERVED = "reserved";

  private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

  private final StringRedisTemplate redisTemplate;
  private final String applicationName;
  private final Duration ttl;

  public IdempotencyService(
      StringRedisTemplate redisTemplate,
      @Value("${spring.application.name}") String applicationName,
      @Value("${wallet.idempotency.ttl}") Duration ttl) {
    this.redisTemplate = redisTemplate;
    this.applicationName = applicationName;
    this.ttl = ttl;
  }

  public boolean reserve(String requestUri, String idempotencyKey) {
    try {
      return TRUE.equals(
          redisTemplate
              .opsForValue()
              .setIfAbsent(keyOf(requestUri, idempotencyKey), RESERVED, ttl));
    } catch (DataAccessException ex) {
      log.warn(LOG_PREFIX + "Redis unavailable, falling back to the database", ex);
      return true;
    }
  }

  private String keyOf(String requestUri, String idempotencyKey) {
    return KEY_FORMAT.formatted(applicationName, requestUri, idempotencyKey);
  }
}
