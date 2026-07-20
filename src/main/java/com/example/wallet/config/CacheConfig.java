package com.example.wallet.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

  private static final String LOG_PREFIX = "[CACHE_CONFIG] ";

  private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

  @Value("${spring.cache.redis.time-to-live:24h}")
  private String cacheTimeToLive;

  @PostConstruct
  public void validateCacheTimeToLive() {
    var ttl = DurationStyle.detectAndParse(cacheTimeToLive);
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalStateException(
          "spring.cache.redis.time-to-live must be a positive duration, but was "
              + cacheTimeToLive);
    }
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {

      @Override
      public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
        log.warn(
            LOG_PREFIX + "Cache read failed, falling back to relational store | cache={}, key={}",
            cache.getName(),
            key,
            ex);
      }

      @Override
      public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
        log.warn(LOG_PREFIX + "Cache write failed | cache={}, key={}", cache.getName(), key, ex);
      }

      @Override
      public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
        log.warn(LOG_PREFIX + "Cache evict failed | cache={}, key={}", cache.getName(), key, ex);
      }

      @Override
      public void handleCacheClearError(RuntimeException ex, Cache cache) {
        log.warn(LOG_PREFIX + "Cache clear failed | cache={}", cache.getName(), ex);
      }
    };
  }
}
